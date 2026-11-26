/*
 * TBalance v3.8.0: registro ágil de productos.
 *
 *  - El precio de venta se calcula solo con el margen del negocio (botones 20, 30, 40, 50 %).
 *  - Escanear el código avisa si el producto ya existe, antes de crear otro igual.
 *  - "Guardar y agregar otro" (Ctrl + Enter) deja el formulario listo con la misma categoría y proveedor.
 *  - "Copiar uno parecido" y "Varios desde Excel" (pegar filas) completan el registro de uno en uno.
 *
 * Se carga después de tbalance-gastos.js: envuelve openModalProducto, editarProducto,
 * closeModal y submitProducto sin reescribirlos. Todo el cálculo y el guardado están en Java.
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
    $('al-contador').textContent = guardadosEnSesion + (guardadosEnSesion === 1 ? ' guardado' : ' guardados') + ' en esta tanda';
    $('prod-nombre').focus();
  }

  // ── Varios desde Excel ──────────────────────────────────────────────

  function abrirLote() {
    $('lote-texto').value = '';
    $('lote-resultado').innerHTML = '';
    $('lote-error').classList.add('hidden');
    $('lote-cargar').disabled = true;
    ultimoLote = null;
    openModal('modal-lote');
    setTimeout(function () { $('lote-texto').focus(); }, 80);
  }
  window.alAbrirLote = abrirLote;

  window.alLoteCambio = function () {
    $('lote-cargar').disabled = true;
    ultimoLote = null;
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
