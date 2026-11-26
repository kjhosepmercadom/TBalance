/*
 * TBalance v3.8.0: registro ágil de productos.
 *
 *  - El precio de venta se calcula solo con el margen del negocio (botones 20, 30, 40, 50 %).
 *  - Escanear el código avisa si el producto ya existe, antes de crear otro igual.
 *  - "Guardar y agregar otro" (Ctrl + Enter) deja el formulario listo con la misma categoría y proveedor.
 *  - "Copiar uno parecido" y "Varios desde Excel" (pegar filas) completan el registro de uno en uno.
 *
 *  - v3.9.0: foto opcional del producto (boutique y tecnología) y vista previa "así lo verá el cajero".
 *    La foto se reduce aquí a ~400 px (WebP, o JPG si el navegador no hace WebP) y pesa unos 30 KB.
 *
 * Se carga después de tbalance-gastos.js: envuelve openModalProducto, editarProducto,
 * closeModal y actualizarUtilidadForm sin reescribirlos. Todo el cálculo y el guardado están en Java.
 */
(function () {
  'use strict';

  var $ = function (id) { return document.getElementById(id); };
  var politica = null;          // { margenPct, redondeo, opcionesRedondeo }
  var margenElegido = null;     // null = el margen del negocio
  var precioAutomatico = true;  // el precio lo puso TBalance y se recalcula al cambiar el costo
  var editando = false;
  var seguirAgregando = false;
  var guardadosEnSesion = 0;
  var temporizador = null;
  var ultimoLote = null;

  function puente(nombre) {
    try {
      var args = Array.prototype.slice.call(arguments, 1);
      return window.javaBridge[nombre].apply(window.javaBridge, args);
    } catch (e) { return null; }
  }
  function leer(texto) {
    try { return JSON.parse(texto); } catch (e) { return null; }
  }
  function pesos(n) { return '$' + Math.round(n).toLocaleString('es-CO'); }
  function enviar(valor) { var n = parseFloat(valor); return isFinite(n) ? n.toFixed(2) : ''; }
  function texto(s) { return typeof esc === 'function' ? esc(s) : String(s == null ? '' : s).replace(/[&<>"]/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]; }); }

  // ── Margen y precio sugerido ────────────────────────────────────────

  function cargarPolitica() {
    var p = leer(puente('obtenerPoliticaPrecio'));
    if (p && !p.error && p.margenPct) { politica = p; }
  }

  window.alProgramarSugerencia = function () {
    clearTimeout(temporizador);
    temporizador = setTimeout(pedirSugerencia, 180);
  };

  window.alPrecioEscrito = function () {
    precioAutomatico = false;
    marcarChips(null);
    var nota = $('al-margen-nota');
    if (nota) { nota.textContent = 'Precio escrito por usted. Toque un margen para que TBalance lo calcule.'; }
  };

  function pedirSugerencia() {
    var compra = parseFloat($('prod-precio-compra').value);
    if (!(compra > 0)) { pintarMargenes(null); return; }
    var s = leer(puente('sugerirPrecio', enviar(compra), enviar($('prod-costo-oculto').value || 0)));
    if (!s || s.error) { pintarMargenes(null); return; }
    pintarMargenes(s);
    if (precioAutomatico) { aplicarMargen(s, margenElegido); }
  }

  function opcionPara(s, margen) {
    if (margen == null) { return { margenPct: s.margenPct, precio: s.precioSugerido }; }
    for (var i = 0; i < s.opciones.length; i++) {
      if (s.opciones[i].margenPct === margen) { return s.opciones[i]; }
    }
    return { margenPct: s.margenPct, precio: s.precioSugerido };
  }

  function aplicarMargen(s, margen) {
    var o = opcionPara(s, margen);
    if (!(o.precio > 0)) { return; }
    $('prod-precio').value = o.precio;
    precioAutomatico = true;
    alLlenoSet('precio', 'Precio de venta con su margen del ' + Math.round(o.margenPct) + ' %');
    actualizarUtilidadForm();
    marcarChips(margen == null ? 'negocio' : margen);
    var nota = $('al-margen-nota');
    if (nota) {
      nota.textContent = 'Precio calculado con margen del ' + Math.round(o.margenPct) + ' %'
        + (politica ? ' y redondeo «' + (politica.opcionesRedondeo[politica.redondeo] || '') + '»' : '') + '. Puede escribir otro.';
    }
  }

  function pintarMargenes(s) {
    var caja = $('al-margenes');
    if (!caja) { return; }
    var html = '';
    if (s) {
      html += '<button type="button" class="al-chip" data-m="negocio" aria-pressed="false">Mi margen ' + Math.round(s.margenPct) + ' % · ' + pesos(s.precioSugerido) + '</button>';
      s.opciones.forEach(function (o) {
        if (o.margenPct === Math.round(s.margenPct)) { return; }
        html += '<button type="button" class="al-chip" data-m="' + o.margenPct + '" aria-pressed="false">' + o.margenPct + ' % · ' + pesos(o.precio) + '</button>';
      });
    }
    html += '<button type="button" class="al-link" onclick="alAlternarPolitica()">Cambiar mi margen</button>';
    caja.innerHTML = html;
    Array.prototype.forEach.call(caja.querySelectorAll('.al-chip'), function (b) {
      b.addEventListener('click', function () {
        var m = b.getAttribute('data-m');
        margenElegido = m === 'negocio' ? null : parseInt(m, 10);
        var actual = leer(puente('sugerirPrecio', enviar($('prod-precio-compra').value), enviar($('prod-costo-oculto').value || 0)));
        if (actual && !actual.error) { aplicarMargen(actual, margenElegido); }
      });
    });
    if (s && precioAutomatico) { marcarChips(margenElegido == null ? 'negocio' : margenElegido); }
  }

  function marcarChips(m) {
    var caja = $('al-margenes');
    if (!caja) { return; }
    Array.prototype.forEach.call(caja.querySelectorAll('.al-chip'), function (b) {
      b.setAttribute('aria-pressed', m != null && b.getAttribute('data-m') === String(m) ? 'true' : 'false');
    });
  }

  window.alAlternarPolitica = function () {
    var panel = $('al-politica');
    if (!panel.hidden) { panel.hidden = true; return; }
    cargarPolitica();
    if (!politica) { return; }
    $('al-pol-margen').value = politica.margenPct;
    $('al-pol-redondeo').innerHTML = Object.keys(politica.opcionesRedondeo).map(function (k) {
      return '<option value="' + k + '"' + (k === politica.redondeo ? ' selected' : '') + '>' + texto(politica.opcionesRedondeo[k]) + '</option>';
    }).join('');
    panel.hidden = false;
    $('al-pol-margen').focus();
  };

  window.alGuardarPolitica = function () {
    var r = puente('guardarPoliticaPrecio', String($('al-pol-margen').value), $('al-pol-redondeo').value);
    if (r === 'OK') {
      $('al-politica').hidden = true;
      cargarPolitica();
      margenElegido = null;
      precioAutomatico = true;
      pedirSugerencia();
      showToast('Margen guardado.');
    } else {
      showToast(r || 'No se pudo guardar el margen.', 'error');
    }
  };

  // ── Código de barras ────────────────────────────────────────────────

  function revisarCodigo() {
    var aviso = $('al-codigo-aviso');
    var codigo = $('prod-codigo').value.trim();
    aviso.hidden = true;
    if (!codigo) { return false; }
    var p = leer(puente('buscarProductoPorCodigo', codigo));
    if (!p || p.error || !p.id || p.id === $('prod-id').value) { return false; }
    aviso.innerHTML = '<span>Ese código ya es de <b>' + texto(p.nombre) + '</b> (hay ' + texto(p.stock) + ').</span>'
      + '<button type="button" class="al-link" data-id="' + texto(p.id) + '">Abrir ese producto</button>'
      + '<button type="button" class="al-link" data-otro="1">Es otro producto</button>';
    aviso.hidden = false;
    aviso.querySelector('[data-id]').onclick = function () { editarProducto(p.id); };
    aviso.querySelector('[data-otro]').onclick = function () { aviso.hidden = true; $('prod-precio-compra').focus(); };
    return true;
  }

  // ── Pestañas: uno, copiar, varios ───────────────────────────────────

  window.alTab = function (cual) {
    ['uno', 'copiar', 'lote'].forEach(function (t) { $('al-tab-' + t).setAttribute('aria-pressed', t === cual ? 'true' : 'false'); });
    $('al-copiar').hidden = cual !== 'copiar';
    if (cual === 'lote') {
      closeModal('modal-producto');
      abrirLote();
      return;
    }
    if (cual === 'copiar') {
      var sel = $('al-copiar-sel');
      var lista = (state.productos || []).filter(function (p) { return p.activo !== false; })
        .slice().sort(function (a, b) { return (a.nombre || '').localeCompare(b.nombre || '', 'es'); });
      sel.innerHTML = '<option value="">Elija un producto...</option>' + lista.map(function (p) {
        return '<option value="' + texto(p.id) + '">' + texto(p.nombre) + '</option>';
      }).join('');
      sel.focus();
    } else {
      $('prod-nombre').focus();
    }
  };

  window.alCopiarDesde = function (id) {
    if (!id) { return; }
    var p = leer(puente('plantillaDesdeProducto', id));
    if (!p || p.error) { showToast((p && p.error) || 'No se pudo copiar el producto.', 'error'); return; }
    var origen = $('al-copiar-sel').selectedOptions[0].text;
    window.openModalProducto();
    $('prod-nombre').value = p.nombre || '';
    $('prod-categoria').value = p.categoriaId || '';
    $('prod-proveedor').value = p.proveedorId || '';
    $('prod-precio-compra').value = p.precioCompra || '';
    $('prod-precio').value = p.precioVenta || '';
    $('prod-costo-oculto').value = p.costoOculto || '';
    $('prod-stock-min').value = p.stockMin || '';
    $('prod-desc').value = p.descripcion || '';
    precioAutomatico = false;
    tbProdEstablecer(p.manejo, p.unidadMedida, p.variantes);
    actualizarUtilidadForm();
    alProgramarSugerencia();
    $('al-tab-uno').setAttribute('aria-pressed', 'false');
    $('al-tab-copiar').setAttribute('aria-pressed', 'true');
    $('modal-prod-title').textContent = 'Agregar producto (copia de ' + origen + ')';
    alLlenoSet('copia', 'Categoría, precios' + (p.manejo === 'VARIANTES' ? ', tallas y colores' : '') + ' de «' + origen + '»');
    alPintarRail();
    var nombre = $('prod-nombre');
    nombre.focus();
    nombre.select();
  };

  // ── Guardar y agregar otro ──────────────────────────────────────────

  window.alGuardarYOtro = function () {
    seguirAgregando = true;
    try { submitProducto(); } finally { seguirAgregando = false; }
  };

  function limpiarParaOtro() {
    guardadosEnSesion++;
    ['prod-id', 'prod-nombre', 'prod-codigo', 'prod-precio-compra', 'prod-precio', 'prod-costo-oculto', 'prod-stock', 'prod-desc'].forEach(function (id) {
      var el = $(id); if (el) { el.value = ''; }
    });
    tbVariantes = (tbVariantes || []).filter(function (v) { return v.talla || v.color; }).map(function (v) {
      return Object.assign({}, v, { id: '', stock: 0 });
    });
    tbProdAplicarFormulario();
    margenElegido = null;
    precioAutomatico = true;
    $('al-codigo-aviso').hidden = true;
    $('prod-error').classList.add('hidden');
    $('modal-prod-title').textContent = 'Agregar producto';
    pintarMargenes(null);
    $('al-margen-nota').textContent = 'Escriba el precio de compra y TBalance calcula el de venta con su margen.';
    actualizarUtilidadForm();
    reiniciarFoto();
    lleno = {};
    alLlenoSet('ultimo', 'Categoría' + (($('prod-proveedor') || {}).value ? ' y proveedor' : '') + ' del producto anterior');
    if (tbVariantes.length) { alLlenoSet('tallas', 'Tallas y colores del producto anterior, con el stock en cero'); }
    alPintarRail();
    $('al-contador').textContent = guardadosEnSesion + (guardadosEnSesion === 1 ? ' guardado' : ' guardados') + ' en esta tanda';
    $('prod-nombre').focus();
  }

  // ── Varios desde Excel ──────────────────────────────────────────────

  function abrirLote() {
    $('lote-texto').value = '';
    $('lote-resultado').innerHTML = '';
    $('lote-error').classList.add('hidden');
    $('lote-cargar').disabled = true;
    $('lote-archivo').value = '';
    $('lote-archivo-nombre').textContent = 'o pegue las filas abajo (.xlsx o .csv)';
    ultimoLote = null;
    openModal('modal-lote');
    setTimeout(function () { $('lote-texto').focus(); }, 80);
  }
  window.alAbrirLote = abrirLote;

  window.alLoteCambio = function () {
    $('lote-cargar').disabled = true;
    ultimoLote = null;
  };

  // Archivo de Excel: Java lo convierte en filas y pasa por la misma revisión que un texto pegado.
  window.alArchivoLote = function (input) {
    var f = input.files && input.files[0];
    if (!f) { return; }
    var err = $('lote-error');
    err.classList.add('hidden');
    if (f.size > 5 * 1024 * 1024) {
      err.textContent = 'El archivo es muy grande (máximo 5 MB).';
      err.classList.remove('hidden');
      return;
    }
    var lector = new FileReader();
    lector.onload = function () {
      var base64 = String(lector.result).split(',')[1] || '';
      var r = leer(puente('leerArchivoLista', f.name, base64));
      if (!r || r.error) {
        err.textContent = (r && r.error) || 'No se pudo leer el archivo.';
        err.classList.remove('hidden');
        return;
      }
      $('lote-texto').value = r.texto;
      $('lote-archivo-nombre').textContent = f.name;
      window.alLoteCambio();
      window.alRevisarLote();
    };
    lector.onerror = function () {
      err.textContent = 'No se pudo abrir el archivo.';
      err.classList.remove('hidden');
    };
    lector.readAsDataURL(f);
  };

  function mostrarLote(r, final) {
    var etiqueta = { LISTA: ['ok', 'Se cargará'], CREADO: ['ok', 'Cargado'], DUPLICADO: ['dup', 'Ya existe'], ERROR: ['err', 'Revisar'] };
    var filas = r.filas.map(function (f) {
      var e = etiqueta[f.estado] || ['err', f.estado];
      return '<tr><td class="n">' + f.linea + '</td><td>' + texto(f.nombre) + '</td><td class="n">' + (f.precio ? pesos(f.precio) : '') + '</td>'
        + '<td class="n">' + (f.cantidad != null ? texto(f.cantidad) : '') + '</td>'
        + '<td><span class="al-estado ' + e[0] + '">' + e[1] + '</span>' + (f.mensaje ? '<div class="al-ayuda">' + texto(f.mensaje) + '</div>' : '') + '</td></tr>';
    }).join('');
    var resumen = final
      ? 'Se cargaron ' + r.creadas + (r.creadas === 1 ? ' producto' : ' productos') + '.'
      : r.listas + (r.listas === 1 ? ' producto se cargará' : ' productos se cargarán');
    var otros = [];
    if (r.duplicados) { otros.push(r.duplicados + ' ya existían'); }
    if (r.errores) { otros.push(r.errores + (r.errores === 1 ? ' tiene error' : ' tienen error')); }
    $('lote-resultado').innerHTML = '<p class="al-resumen">' + resumen + (otros.length ? ' · ' + otros.join(' · ') : '') + '</p>'
      + '<div style="max-height:260px;overflow:auto"><table class="al-tabla"><thead><tr><th class="n">Fila</th><th>Producto</th><th class="n">Precio</th><th class="n">Cant.</th><th>Estado</th></tr></thead><tbody>' + filas + '</tbody></table></div>';
  }

  function correrLote(nombre, final) {
    var err = $('lote-error');
    err.classList.add('hidden');
    var bruto = puente(nombre, $('lote-texto').value);
    var r = leer(bruto);
    if (!r || r.error || !r.filas) {
      err.textContent = (r && r.error) || (bruto && String(bruto).replace(/^ERROR: /, '')) || 'No se pudo leer la lista.';
      err.classList.remove('hidden');
      return null;
    }
    mostrarLote(r, final);
    return r;
  }

  window.alRevisarLote = function () {
    ultimoLote = correrLote('revisarListaProductos', false);
    $('lote-cargar').disabled = !(ultimoLote && ultimoLote.listas > 0);
    if (ultimoLote && ultimoLote.listas > 0) { $('lote-cargar').textContent = 'Cargar ' + ultimoLote.listas + (ultimoLote.listas === 1 ? ' producto' : ' productos'); }
  };

  window.alCargarLote = function () {
    var r = correrLote('importarListaProductos', true);
    if (!r) { return; }
    $('lote-cargar').disabled = true;
    $('lote-cargar').textContent = 'Cargar productos';
    if (r.creadas > 0) {
      cargarEstadoDesdeJava();
      showToast('Se cargaron ' + r.creadas + (r.creadas === 1 ? ' producto.' : ' productos.'));
    }
  };


  // ── Foto del producto (v3.9.0) ──────────────────────────────────────

  var LADO_FOTO = 400;           // px del lado más largo
  var PESO_OBJETIVO = 45 * 1024; // el servicio rechaza más de 60 KB
  var fotos = { habilitada: false, ids: {}, nueva: null, tenia: false, quitar: false, productoId: '' };

  function cargarEstadoFotos() {
    var r = leer(puente('obtenerEstadoFotos'));
    fotos.habilitada = !!(r && !r.error && r.habilitadas);
    fotos.ids = {};
    if (r && r.ids) { r.ids.forEach(function (id) { fotos.ids[id] = true; }); }
    // Si el negocio no usa fotos (o el puente no las conoce) no se muestra nada
    var caja = $('al-foto');
    if (caja) { caja.hidden = !fotos.habilitada; }
    var activar = $('al-foto-activar'), apagar = $('al-foto-apagar');
    if (activar) { activar.hidden = fotos.habilitada || !r || !!r.error; }
    if (apagar) { apagar.hidden = !fotos.habilitada; }
  }
  window.tbFotoTiene = function (id) { return !!(fotos.habilitada && fotos.ids[id]); };

  function mensajeFoto(texto) {
    var m = $('al-foto-msg');
    if (!m) { return; }
    m.textContent = texto || '';
    m.hidden = !texto;
  }

  function pintarFoto() {
    var url = fotos.nueva;
    var img = $('al-foto-img');
    var hay = !!url;
    img.hidden = !hay;
    if (hay) { img.src = url; } else { img.removeAttribute('src'); }
    $('al-foto-vacia').hidden = hay;
    $('al-foto-acciones').hidden = !hay;
  }

  function reiniciarFoto() {
    fotos.nueva = null; fotos.guardada = null; fotos.tenia = false; fotos.quitar = false; fotos.productoId = '';
    mensajeFoto('');
    if ($('al-foto-img')) { pintarFoto(); }
  }

  function cargarFotoGuardada(id) {
    fotos.productoId = id;
    if (!fotos.habilitada || !fotos.ids[id]) { return; }
    fotos.tenia = true;
    var url = puente('obtenerFotoProducto', id);
    if (url && String(url).indexOf('data:image/') === 0) { fotos.nueva = url; fotos.guardada = url; pintarFoto(); }
  }

  window.alElegirFoto = function () { $('al-foto-input').click(); };

  window.alQuitarFoto = function () {
    fotos.nueva = null;
    fotos.quitar = fotos.tenia;
    mensajeFoto('');
    pintarFoto();
  };

  function pesoDe(dataUrl) { return Math.round((dataUrl.length - dataUrl.indexOf(',') - 1) * 0.75); }

  // Reduce la imagen: lado largo 400 px y, si hace falta, menos calidad hasta pesar ≤ 45 KB.
  function reducirFoto(archivo) {
    return new Promise(function (ok, fallo) {
      var visor = new Image();
      var origen = URL.createObjectURL(archivo);
      visor.onload = function () {
        URL.revokeObjectURL(origen);
        var lado = LADO_FOTO;
        for (var vuelta = 0; vuelta < 5; vuelta++) {
          var escala = Math.min(1, lado / Math.max(visor.naturalWidth, visor.naturalHeight));
          var ancho = Math.max(1, Math.round(visor.naturalWidth * escala));
          var alto = Math.max(1, Math.round(visor.naturalHeight * escala));
          var lienzo = document.createElement('canvas');
          lienzo.width = ancho; lienzo.height = alto;
          var c = lienzo.getContext('2d');
          c.fillStyle = '#fff';                // los PNG con fondo transparente quedan sobre blanco
          c.fillRect(0, 0, ancho, alto);
          c.drawImage(visor, 0, 0, ancho, alto);
          var tipo = 'image/webp';
          var probe = lienzo.toDataURL(tipo, 0.8);
          if (probe.indexOf('data:image/webp') !== 0) { tipo = 'image/jpeg'; }
          for (var q = 0.85; q >= 0.4; q -= 0.15) {
            var url = lienzo.toDataURL(tipo, q);
            if (pesoDe(url) <= PESO_OBJETIVO) { ok(url); return; }
          }
          lado = Math.round(lado * 0.8);
        }
        fallo(new Error('No se pudo reducir esa foto. Pruebe con otra.'));
      };
      visor.onerror = function () { URL.revokeObjectURL(origen); fallo(new Error('Ese archivo no se puede leer como foto. Elija un JPG, PNG o WebP.')); };
      visor.src = origen;
    });
  }

  function tomarArchivoFoto(archivo) {
    if (!archivo) { return; }
    if (!/^image\//.test(archivo.type || '')) { mensajeFoto('Elija una foto en JPG, PNG o WebP.'); return; }
    mensajeFoto('Reduciendo la foto…');
    reducirFoto(archivo).then(function (url) {
      fotos.nueva = url;
      fotos.quitar = false;
      pintarFoto();
      mensajeFoto('Lista: pesa ' + Math.max(1, Math.round(pesoDe(url) / 1024)) + ' KB.');
    }, function (e) { mensajeFoto(e.message); });
  }

  // Cambia la foto solo si la persona la cambió o la quitó; si no, el guardado es el de siempre.
  window.alGuardarConFoto = function (producto) {
    var json = JSON.stringify(producto);
    var cambio = fotos.habilitada && (fotos.quitar || (fotos.nueva && fotos.nueva !== fotos.guardada));
    if (!cambio) { return window.javaBridge.guardarProducto(json); }
    var r = window.javaBridge.guardarProductoConFoto(json, fotos.quitar ? 'QUITAR' : fotos.nueva);
    if (producto.id) { delete miniaturas[producto.id]; }
    if (typeof r === 'string' && r.indexOf('OK_SIN_FOTO: ') === 0) {
      showToast('El producto se guardó, pero la foto no: ' + r.substring(13), 'error');
      return 'OK';
    }
    return r;
  };

  window.alUsarFotos = function (usar) {
    var r = puente('usarFotosProducto', usar ? 'true' : 'false');
    if (r === 'OK') {
      cargarEstadoFotos();
      showToast(usar ? 'Ya puede poner foto a sus productos.' : 'Las fotos quedaron apagadas. Las que ya subió se conservan.');
    } else {
      showToast(r || 'No se pudo cambiar.', 'error');
    }
  };


  // ── Miniaturas en la lista de productos ─────────────────────────────

  var miniaturas = {};   // id → data URL ya traída (la foto se pide una vez y solo si el producto está en pantalla)

  function ponerMiniaturas() {
    cargarEstadoFotos();
    if (!fotos.habilitada) { return; }
    Array.prototype.forEach.call(document.querySelectorAll('#tabla-productos tr'), function (fila, i) {
      var celdaId = fila.children[0], celdaNombre = fila.children[2];
      if (!celdaId || !celdaNombre || celdaNombre.querySelector('.al-mini-fila')) { return; }
      var id = (celdaId.textContent || '').trim().replace(/^#/, '');
      if (!fotos.ids[id]) { return; }
      var caja = document.createElement('div');
      caja.className = 'al-mini-fila';
      var img = document.createElement('img');
      img.className = 'al-mini';
      img.alt = '';
      var textos = document.createElement('div');
      while (celdaNombre.firstChild) { textos.appendChild(celdaNombre.firstChild); }
      caja.appendChild(img);
      caja.appendChild(textos);
      celdaNombre.appendChild(caja);
      setTimeout(function () {
        if (!miniaturas[id]) {
          var url = puente('obtenerFotoProducto', id);
          if (url && String(url).indexOf('data:image/') === 0) { miniaturas[id] = url; }
        }
        if (miniaturas[id]) { img.src = miniaturas[id]; } else { img.remove(); }
      }, 20 * i);
    });
  }

  var tablaOriginal = window.renderTablaProductos;
  if (typeof tablaOriginal === 'function') {
    window.renderTablaProductos = function () {
      var r = tablaOriginal.apply(this, arguments);
      try { ponerMiniaturas(); } catch (e) { /* la lista nunca debe quedar vacía por una miniatura */ }
      return r;
    };
  }

  // ── Vista previa "así lo verá el cajero" ────────────────────────────

  var lleno = {};   // lo que TBalance completó solo, en orden

  window.alLlenoSet = function (clave, texto) {
    if (texto) { lleno[clave] = texto; } else { delete lleno[clave]; }
    var claves = Object.keys(lleno);
    $('al-lleno').hidden = claves.length === 0;
    $('al-lleno-lista').innerHTML = claves.map(function (k) { return '<li><i aria-hidden="true">✓</i><span>' + texto_(lleno[k]) + '</span></li>'; }).join('');
  };
  function texto_(t) { return texto(t); }

  function numero(id) { var n = parseFloat(($(id) || {}).value); return isFinite(n) ? n : 0; }

  window.alPintarRail = function () {
    var nombre = ($('prod-nombre').value || '').trim();
    var precio = numero('prod-precio'), costo = numero('prod-precio-compra') + numero('prod-costo-oculto');
    $('al-pv-nombre').textContent = nombre || 'Nombre del producto';
    $('al-pv-nombre').classList.toggle('vacio', !nombre);
    $('al-pv-precio').textContent = pesos(precio);
    var cat = $('prod-categoria');
    var partes = [];
    if (cat && cat.value) { partes.push(cat.options[cat.selectedIndex].text.replace(/ \(deshabilitada\)$/, '')); }
    if ($('prod-manejo').value === 'VARIANTES') {
      var vs = (typeof tbVariantes !== 'undefined' ? tbVariantes : []).filter(function (v) { return v.talla || v.color; });
      var tallas = {}, colores = {};
      vs.forEach(function (v) { if (v.talla) { tallas[v.talla] = 1; } if (v.color) { colores[v.color] = 1; } });
      var nc = Object.keys(colores).length, nt = Object.keys(tallas).length;
      if (nc) { partes.push(nc + (nc === 1 ? ' color' : ' colores')); }
      if (nt) { partes.push(nt + (nt === 1 ? ' talla' : ' tallas')); }
    }
    $('al-pv-pista').textContent = partes.join(' · ');
    var ring = $('al-ring');
    if (precio > 0 && costo > 0) {
      var gan = precio - costo;
      var pct = Math.max(0, Math.min(100, gan / precio * 100));
      ring.style.setProperty('--v', pct.toFixed(1));
      ring.classList.toggle('neg', gan < 0);
      $('al-ring-txt').textContent = Math.round(gan / precio * 100) + ' %';
      $('al-ring-gan').textContent = (gan < 0 ? '−' : '') + pesos(Math.abs(gan));
      $('al-ring-nota').textContent = gan < 0 ? 'de pérdida por unidad: cobra menos de lo que le cuesta' : 'de ganancia por unidad';
    } else {
      ring.style.setProperty('--v', 0);
      ring.classList.remove('neg');
      $('al-ring-txt').textContent = '—';
      $('al-ring-gan').textContent = '$ 0';
      $('al-ring-nota').textContent = 'Escriba el costo y el precio para ver la ganancia';
    }
  };

  // ── Enganche con el modal de siempre ────────────────────────────────

  function modoEdicion(si) {
    editando = si;
    $('al-tabs').style.display = si ? 'none' : '';
    $('al-copiar').hidden = true;
    $('prod-guardar-otro').style.display = si ? 'none' : '';
    $('al-contador').style.display = si ? 'none' : '';
    $('al-mas').open = si;
    $('al-politica').hidden = true;
  }

  var abrirOriginal = window.openModalProducto;
  window.openModalProducto = function () {
    abrirOriginal.apply(this, arguments);
    modoEdicion(false);
    guardadosEnSesion = 0;
    $('al-contador').textContent = '';
    $('al-tab-uno').setAttribute('aria-pressed', 'true');
    $('al-tab-copiar').setAttribute('aria-pressed', 'false');
    $('al-tab-lote').setAttribute('aria-pressed', 'false');
    $('al-codigo-aviso').hidden = true;
    margenElegido = null;
    precioAutomatico = true;
    cargarPolitica();
    pintarMargenes(null);
    $('al-margen-nota').textContent = 'Escriba el precio de compra y TBalance calcula el de venta con su margen.';
    lleno = {};
    alLlenoSet();
    reiniciarFoto();
    cargarEstadoFotos();
    alPintarRail();
  };

  var editarOriginal = window.editarProducto;
  window.editarProducto = function () {
    editarOriginal.apply(this, arguments);
    modoEdicion(true);
    precioAutomatico = false;
    margenElegido = null;
    cargarPolitica();
    pintarMargenes(null);
    $('al-margen-nota').textContent = 'Toque un margen para recalcular el precio de venta.';
    alProgramarSugerencia();
    lleno = {};
    alLlenoSet();
    reiniciarFoto();
    cargarEstadoFotos();
    cargarFotoGuardada($('prod-id').value);
    alPintarRail();
  };

  // La vista previa sigue cada cambio de precio, costo, nombre, categoría o tallas.
  var utilidadOriginal = window.actualizarUtilidadForm;
  window.actualizarUtilidadForm = function () {
    var r = utilidadOriginal.apply(this, arguments);
    alPintarRail();
    return r;
  };

  var cerrarOriginal = window.closeModal;
  window.closeModal = function (id) {
    if (id === 'modal-producto' && seguirAgregando) {
      limpiarParaOtro();
      return;
    }
    return cerrarOriginal.apply(this, arguments);
  };

  document.addEventListener('DOMContentLoaded', function () {
    var codigo = $('prod-codigo');
    if (codigo) {
      codigo.addEventListener('keydown', function (e) {
        if (e.key !== 'Enter') { return; }
        e.preventDefault();               // el lector termina con Enter: no debe guardar el formulario
        if (!revisarCodigo()) { $('prod-precio-compra').focus(); }
      });
      codigo.addEventListener('change', revisarCodigo);
    }
    ['prod-nombre', 'prod-categoria', 'prod-manejo'].forEach(function (id) {
      var el = $(id);
      if (el) { el.addEventListener('input', alPintarRail); el.addEventListener('change', alPintarRail); }
    });
    var entrada = $('al-foto-input');
    if (entrada) {
      entrada.addEventListener('change', function () { tomarArchivoFoto(entrada.files && entrada.files[0]); entrada.value = ''; });
    }
    var zona = $('al-foto');
    if (zona) {
      ['dragenter', 'dragover'].forEach(function (n) { zona.addEventListener(n, function (e) { e.preventDefault(); zona.classList.add('suelta'); }); });
      ['dragleave', 'drop'].forEach(function (n) { zona.addEventListener(n, function (e) { e.preventDefault(); zona.classList.remove('suelta'); }); });
      zona.addEventListener('drop', function (e) { tomarArchivoFoto(e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0]); });
    }
    var modal = $('modal-producto');
    if (modal) {
      modal.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
          e.preventDefault();
          if (editando) { submitProducto(); } else { alGuardarYOtro(); }
        }
      });
    }
  });
})();
