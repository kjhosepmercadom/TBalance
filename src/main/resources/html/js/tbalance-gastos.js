// TBalance - Gastos del negocio, utilidad neta real, ganancia real por producto y comprobante de venta.
// Las cuentas las hace Java (ExpenseService); aquí solo se muestran y se piden los datos.
(function () {
  'use strict';

  const $g = id => document.getElementById(id);
  const escG = t => String(t == null ? '' : t).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const pesosG = n => (Number(n) < 0 ? '- $ ' : '$ ') + Math.round(Math.abs(Number(n) || 0)).toLocaleString('es-CO');
  const pctG = n => String(Math.round((Number(n) || 0) * 10) / 10).replace('.', ',') + ' %';
  const puedeG = p => (typeof window.tbPuede === 'function' ? window.tbPuede(p) : true);

  function leerG(raw) {
    if (typeof raw === 'string' && raw.startsWith('ERROR')) return { error: raw.replace(/^ERROR:\s*/, '') };
    try { return { datos: JSON.parse(raw) }; } catch (e) { return { error: 'No se pudo leer la respuesta.' }; }
  }
  function avisar(msg, tipo) { if (typeof showToast === 'function') showToast(msg, tipo || 'success'); }

  // ════════════════════════════════════════════════════════
  //  Utilidad neta y gastos del mes
  // ════════════════════════════════════════════════════════
  const hoy = new Date();
  const MES_ACTUAL = hoy.getFullYear() + '-' + String(hoy.getMonth() + 1).padStart(2, '0');
  const G = { mes: MES_ACTUAL, gastos: [], resumen: null };

  function sumarMes(periodo, delta) {
    const p = periodo.split('-');
    const d = new Date(Number(p[0]), Number(p[1]) - 1 + delta, 1);
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
  }
  function nombreMes(periodo) {
    const p = periodo.split('-');
    return new Date(Number(p[0]), Number(p[1]) - 1, 1).toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
  }
  const CLASES = { AGUA: 'Agua', LUZ: 'Luz', GAS: 'Gas', INTERNET: 'Internet', ARRIENDO: 'Arriendo', NOMINA: 'Nómina', OTRO: 'Otro' };

  window.tbGastosCargar = function () {
    const bloque = $g('tbg-bloque');
    if (!bloque) return;
    const visible = !!(window.javaBridge && window.javaBridge.obtenerUtilidadNeta) && puedeG('COSTOS');
    bloque.hidden = !visible;
    if (!visible) return;
    const rGastos = leerG(window.javaBridge.obtenerGastos(G.mes));
    const rResumen = leerG(window.javaBridge.obtenerUtilidadNeta(G.mes));
    if (rGastos.error || rResumen.error) {
      $g('tbg-titular').textContent = rGastos.error || rResumen.error;
      return;
    }
    G.gastos = rGastos.datos || [];
    G.resumen = rResumen.datos;
    pintarResumen();
    pintarGastos();
  };

  window.tbGastosMes = function (delta) {
    const nuevo = sumarMes(G.mes, delta);
    if (nuevo > MES_ACTUAL) return;
    G.mes = nuevo;
    window.tbGastosCargar();
  };

  function fila(clase, etiqueta, valor, pct) {
    return '<div class="' + clase + '"><dt>' + etiqueta + '</dt><dd' + (clase.indexOf('total') >= 0 && valor < 0 ? ' class="neg"' : '') + '>'
      + pesosG(valor) + (pct == null ? '' : '<span class="pct">' + pctG(pct) + '</span>') + '</dd></div>';
  }

  function pintarResumen() {
    const r = G.resumen;
    const mes = nombreMes(G.mes);
    $g('tbg-mes-nombre').textContent = mes;
    $g('tbg-mes-sig').disabled = G.mes >= MES_ACTUAL;
    $g('tbg-gastos-mes').textContent = mes;
    const titular = $g('tbg-titular');
    if (r.ingresos === 0 && r.gastos === 0) {
      titular.className = 'tbg-titular';
      titular.textContent = 'No hay ventas ni gastos en ' + mes + '.';
    } else if (r.utilidadNeta >= 0) {
      titular.className = 'tbg-titular';
      titular.innerHTML = 'En ' + escG(mes) + ' le quedaron <b>' + pesosG(r.utilidadNeta) + '</b> después de pagar la mercancía y los gastos.';
    } else {
      titular.className = 'tbg-titular perdida';
      titular.innerHTML = 'En ' + escG(mes) + ' el negocio perdió <b>' + pesosG(-r.utilidadNeta) + '</b>: los gastos fueron mayores que la ganancia de las ventas.';
    }
    let html = fila('', 'Vendió', r.ingresos)
      + fila('resta', 'Costo de la mercancía que vendió', -r.costoMercancia);
    if (r.costosOcultos > 0) html += fila('resta', 'Costos ocultos (empaque, flete, comisión)', -r.costosOcultos);
    html += fila('sub', 'Ganancia de las ventas', r.utilidadBruta, r.margenBrutoPct)
      + fila('resta', 'Gastos del negocio', -r.gastos)
      + fila('total', 'Utilidad neta', r.utilidadNeta, r.ingresos > 0 ? r.margenNetoPct : null);
    $g('tbg-cuenta').innerHTML = html;

    let eq = '';
    if (r.gastosFijos > 0) {
      if (r.puntoEquilibrio == null) {
        eq = 'Este mes las ventas no dejaron ganancia, así que no se puede calcular cuánto hay que vender para cubrir los gastos fijos (' + pesosG(r.gastosFijos) + ').';
      } else if (r.faltaParaEquilibrio > 0) {
        eq = 'Para cubrir los gastos fijos (' + pesosG(r.gastosFijos) + ') hay que vender ' + pesosG(r.puntoEquilibrio) + ' en el mes. Faltan ' + pesosG(r.faltaParaEquilibrio) + '.';
      } else {
        eq = 'Ya se cubrieron los gastos fijos del mes (' + pesosG(r.gastosFijos) + '). Lo que se venda de aquí en adelante es ganancia limpia.';
      }
    }
    $g('tbg-equilibrio').textContent = eq;
    $g('tbg-equilibrio').hidden = !eq;
    $g('tbg-aviso').textContent = r.ventasSinCosto > 0
      ? r.ventasSinCosto + (r.ventasSinCosto === 1 ? ' venta de este mes no tiene' : ' ventas de este mes no tienen') + ' el precio de compra guardado: la ganancia de las ventas puede verse más alta de lo real.'
      : '';
  }

  function pintarGastos() {
    const lista = $g('tbg-lista');
    if (!G.gastos.length) {
      lista.innerHTML = '<p class="tbg-vacio">Todavía no hay gastos en este mes. Agregue la luz, el agua, el arriendo o la nómina para ver cuánto le queda de verdad.</p>';
      return;
    }
    const total = G.gastos.reduce((t, x) => t + (x.monto || 0), 0);
    lista.innerHTML = G.gastos.map(function (x) {
      const clase = CLASES[x.categoria] || 'Gasto';
      const nota = [x.descripcion, 'Pagado el ' + String(x.fechaPago || '').split('-').reverse().join('/')].filter(Boolean).join(' · ');
      const etq = x.recurrente ? '<span class="tbg-etq">Se repite cada mes</span>' : (x.origenId ? '<span class="tbg-etq">Gasto fijo</span>' : '');
      return '<div class="tbg-fila"><div class="quien">' + escG(clase) + etq + '<small>' + escG(nota) + '</small></div>'
        + '<span class="valor">' + pesosG(x.monto) + '</span>'
        + '<span class="acc"><button type="button" onclick="tbGastoAbrir(\'' + escG(x.id) + '\')">Corregir</button>'
        + '<button type="button" onclick="tbGastoQuitar(\'' + escG(x.id) + '\')">Quitar</button></span></div>';
    }).join('') + '<div class="tbg-total-gastos"><span>Total de gastos</span><span>' + pesosG(total) + '</span></div>';
  }

  // ── ventana del gasto ─────────────────────────────────────
  let editando = null;
  window.tbGastoAbrir = function (id) {
    editando = id ? G.gastos.find(x => x.id === id) : null;
    const copia = !!(editando && editando.origenId);
    $g('gas-titulo').textContent = editando ? 'Corregir gasto' : 'Agregar gasto';
    $g('gas-clase').value = editando ? editando.categoria : 'LUZ';
    $g('gas-descripcion').value = editando ? (editando.descripcion || '') : '';
    $g('gas-monto').value = editando ? editando.monto : '';
    $g('gas-mes').value = editando ? editando.periodo : G.mes;
    $g('gas-mes').disabled = !!editando;
    $g('gas-fecha').value = editando ? editando.fechaPago : (G.mes === MES_ACTUAL ? MES_ACTUAL + '-' + String(hoy.getDate()).padStart(2, '0') : G.mes + '-01');
    $g('gas-recurrente').checked = !!(editando && editando.recurrente);
    $g('gas-repite-wrap').hidden = copia;
    $g('gas-copia-nota').hidden = !copia;
    $g('gas-error').hidden = true;
    openModal('modal-gasto');
    setTimeout(function () { $g('gas-monto').focus(); }, 60);
  };

  window.tbGastoGuardar = function () {
    const err = $g('gas-error');
    const monto = parseFloat($g('gas-monto').value);
    const mes = $g('gas-mes').value;
    const fecha = $g('gas-fecha').value;
    err.hidden = true;
    if (!mes) { err.textContent = 'Elija el mes del gasto.'; err.hidden = false; return; }
    if (!(monto > 0)) { err.textContent = 'Escriba cuánto costó el gasto.'; err.hidden = false; return; }
    if (!fecha) { err.textContent = 'Escriba la fecha en que se pagó.'; err.hidden = false; return; }
    const gasto = {
      id: editando ? editando.id : undefined, periodo: mes, fechaPago: fecha, categoria: $g('gas-clase').value,
      descripcion: $g('gas-descripcion').value.trim(), monto: monto, recurrente: $g('gas-recurrente').checked
    };
    const r = leerG(window.javaBridge.guardarGasto(JSON.stringify(gasto)));
    if (r.error) { err.textContent = r.error; err.hidden = false; return; }
    closeModal('modal-gasto');
    if (mes <= MES_ACTUAL) G.mes = mes;
    window.tbGastosCargar();
    avisar(editando ? 'Gasto corregido.' : (gasto.recurrente ? 'Gasto guardado. Cada mes se creará solo con este valor.' : 'Gasto guardado.'));
  };

  window.tbGastoQuitar = function (id) {
    const x = G.gastos.find(g => g.id === id);
    if (!x) return;
    $g('confirm-motivo').classList.add('hidden');
    $g('confirm-error').classList.add('hidden');
    $g('confirm-title').textContent = '¿Quitar este gasto?';
    $g('confirm-msg').textContent = (CLASES[x.categoria] || 'Gasto') + ' de ' + pesosG(x.monto) + '.'
      + (x.recurrente ? ' Como se repite cada mes, dejará de copiarse a los meses que siguen.' : ' Deja de contar en la utilidad del mes.');
    const btn = $g('confirm-btn');
    btn.textContent = 'Quitar';
    btn.onclick = function () {
      const r = window.javaBridge.eliminarGasto(id);
      if (typeof r === 'string' && r.startsWith('ERROR')) { avisar(r.replace(/^ERROR:\s*/, ''), 'error'); return; }
      closeModal('modal-confirm');
      window.tbGastosCargar();
      avisar('Gasto quitado.');
    };
    openModal('modal-confirm');
  };

  // abrir Costos carga el mes
  const showAntes = window.showSection;
  window.showSection = function (sec) {
    const r = showAntes.apply(this, arguments);
    if (sec === 'costos') window.tbGastosCargar();
    return r;
  };

  // ════════════════════════════════════════════════════════
  //  Datos del comprobante en la ventana del negocio
  // ════════════════════════════════════════════════════════
  const CAMPOS_NEGOCIO = [['perfil-nit', 'negocio.nit'], ['perfil-direccion', 'negocio.direccion'], ['perfil-telefono', 'negocio.telefono'],
    ['perfil-pie', 'negocio.pie'], ['perfil-carga-gastos', 'gastos.cargaPct']];
  window.tbDatosNegocioMostrar = function (cfg) {
    cfg = cfg || {};
    CAMPOS_NEGOCIO.forEach(function (c) { const el = $g(c[0]); if (el) el.value = cfg[c[1]] || ''; });
  };
  window.tbDatosNegocioComoConfiguracion = function () {
    const cfg = {};
    CAMPOS_NEGOCIO.forEach(function (c) { const el = $g(c[0]); if (el) cfg[c[1]] = el.value.trim(); });
    return cfg;
  };

  // ════════════════════════════════════════════════════════
  //  Ganancia real por producto (formulario de producto)
  // ════════════════════════════════════════════════════════
  let carga = { t: 0, v: null };
  function cargaGastos() {
    if (Date.now() - carga.t < 60000) return carga.v;
    carga = { t: Date.now(), v: null };
    if (!window.javaBridge || !window.javaBridge.obtenerCargaGastos) return null;
    const r = leerG(window.javaBridge.obtenerCargaGastos());
    carga.v = r.error || r.datos == null ? null : Number(r.datos);
    return carga.v;
  }
  function pintarRealProducto() {
    const caja = $g('prod-real');
    if (!caja) return;
    const compra = parseFloat(($g('prod-precio-compra') || {}).value) || 0;
    const venta = parseFloat(($g('prod-precio') || {}).value) || 0;
    const oculto = parseFloat(($g('prod-costo-oculto') || {}).value) || 0;
    if (compra <= 0 || venta <= 0) { caja.hidden = true; return; }
    const bruta = venta - compra;
    const trasOculto = bruta - oculto;
    const pct = cargaGastos();
    let html = '<div><span>Ganancia por unidad (precio de venta - compra)</span><b>' + pesosG(bruta) + '</b></div>';
    if (oculto > 0) html += '<div class="' + (trasOculto < 0 ? 'mal' : '') + '"><span>Después del costo oculto (' + pesosG(oculto) + ')</span><b>' + pesosG(trasOculto) + '</b></div>';
    if (pct != null && pct > 0) {
      const estimada = trasOculto - venta * pct / 100;
      html += '<div class="' + (estimada < 0 ? 'mal' : '') + '"><span>Estimada después de gastos (' + pctG(pct) + ' de lo que se vende)</span><b>' + pesosG(estimada) + '</b></div>';
      html += '<small>' + (estimada < 0 ? 'Con los gastos del negocio, este producto deja pérdida. Revise su precio o su costo.'
        : (venta > 0 && estimada / venta < 0.05 ? 'El margen real es muy bajo: casi todo se va en costos y gastos.' : 'Es un cálculo aproximado: los gastos no se reparten producto por producto.')) + '</small>';
    } else if (oculto > 0) {
      html += '<small>El costo oculto es lo que gasta en empaque, bolsa, flete o comisión por cada unidad que vende.</small>';
    }
    caja.innerHTML = html;
    caja.hidden = false;
  }
  const utilidadBase = window.actualizarUtilidadForm;
  window.actualizarUtilidadForm = function () {
    if (typeof utilidadBase === 'function') utilidadBase.apply(this, arguments);
    pintarRealProducto();
  };

  // ════════════════════════════════════════════════════════
  //  Comprobante de venta (ticket 80 mm y carta / A4)
  // ════════════════════════════════════════════════════════
  const CLAVE_FORMATO = 'tbalance.comprobante.formato';
  const CLAVE_AUTO = 'tbalance.comprobante.alCobrar';
  const UNIDAD = { M: ' m', KG: ' kg', L: ' L', CAJA: ' caja(s)' };
  let venta = null;
  let formato = leerLocal(CLAVE_FORMATO) === 'a4' ? 'a4' : 't80';

  function leerLocal(k) { try { return window.localStorage.getItem(k); } catch (e) { return null; } }
  function guardarLocal(k, v) { try { window.localStorage.setItem(k, v); } catch (e) { /* sin almacenamiento: no pasa nada */ } }

  function cantidadTexto(item) {
    const p = (typeof state !== 'undefined' && state.productos || []).find(x => x.id === item.productoId);
    const n = Number(item.cantidad) || 0;
    return String(n).replace('.', ',') + (p && UNIDAD[p.unidadMedida] ? UNIDAD[p.unidadMedida] : '');
  }
  function horaDe(v) {
    const f = String(v.fecha || '');
    if (f.length > 10) return f.substring(11, 16);
    const r = String(v.fechaRegistro || '');
    return r.length > 10 ? r.substring(11, 16) : '';
  }
  function fechaDe(v) { return String(v.fecha || '').substring(0, 10).split('-').reverse().join('/'); }

  function htmlComprobante(v, fmt) {
    const p = (typeof state !== 'undefined' && state.perfilActivo) || {};
    const cfg = p.configuracion || {};
    const numero = v.numero > 0 ? String(v.numero).padStart(6, '0') : String(v.id || '');
    const items = v.items || [];
    const total = v.total != null ? v.total : items.reduce((t, i) => t + (i.cantidad || 0) * (i.precioUnitario || 0), 0);
    const cabecera = '<h1>' + escG(p.nombre || 'Negocio') + '</h1>'
      + (cfg['negocio.nit'] ? '<div class="centro">NIT ' + escG(cfg['negocio.nit']) + '</div>' : '')
      + (cfg['negocio.direccion'] ? '<div class="centro">' + escG(cfg['negocio.direccion']) + '</div>' : '')
      + (cfg['negocio.telefono'] ? '<div class="centro">Tel. ' + escG(cfg['negocio.telefono']) + '</div>' : '');
    const cliente = v.cliente ? '<div>Cliente: ' + escG(v.cliente) + (v.clienteDocumento ? ' · C.C. ' + escG(v.clienteDocumento) : '') + '</div>' : '';
    const filas = items.map(function (i) {
      const nombre = escG(i.nombreProducto || '') + (i.descripcionVariante ? ' <small>(' + escG(i.descripcionVariante) + ')</small>' : '');
      const sub = (Number(i.cantidad) || 0) * (Number(i.precioUnitario) || 0);
      return '<tr><td>' + escG(cantidadTexto(i)) + '</td><td>' + nombre + '</td><td class="der">' + pesosG(i.precioUnitario) + '</td><td class="der">' + pesosG(sub) + '</td></tr>';
    }).join('');
    const pago = escG(v.metodoPago || 'Efectivo') + (v.referenciaPago ? ' · ' + escG(v.referenciaPago) : '');
    const anulada = v.estado === 'ANULADA' || v.anulada;
    return '<div class="cmp ' + fmt + '">'
      + (anulada ? '<div class="anulada">ANULADA</div>' : '')
      + cabecera + '<hr class="linea">'
      + '<div class="neg centro">COMPROBANTE DE VENTA No. ' + escG(numero) + '</div>'
      + '<div class="meta"><span>' + escG(fechaDe(v)) + ' ' + escG(horaDe(v)) + '</span><span>' + escG(v.vendedor ? 'Atendió: ' + v.vendedor : '') + '</span></div>'
      + cliente + '<hr class="linea">'
      + '<table><thead><tr><th>Cant.</th><th>Descripción</th><th class="der">Precio</th><th class="der">Subtotal</th></tr></thead><tbody>' + filas + '</tbody>'
      + '<tfoot><tr class="total"><td colspan="3">TOTAL</td><td class="der">' + pesosG(total) + '</td></tr></tfoot></table>'
      + '<hr class="linea"><div>Pago: ' + pago + '</div>'
      + (anulada && v.motivoAnulacion ? '<div>Anulada: ' + escG(v.motivoAnulacion) + '</div>' : '')
      + (cfg['negocio.pie'] ? '<div class="pie">' + escG(cfg['negocio.pie']) + '</div>' : '')
      + '<div class="legal">Comprobante de venta. No es factura electrónica de la DIAN.</div>'
      + '</div>';
  }

  function buscarVenta(id) {
    let v = (typeof state !== 'undefined' && state.ventas || []).find(x => x.id === id);
    if (v) return v;
    const r = leerG(window.javaBridge.obtenerVentasAnuladas());
    v = (r.datos || []).find(x => x.id === id);
    if (v) v.estado = 'ANULADA';
    return v || null;
  }

  function pintarVista() {
    if (!venta) return;
    $g('cmp-vista').innerHTML = '<div class="cmp-hoja">' + htmlComprobante(venta, formato) + '</div>';
    document.querySelectorAll('#cmp-formato button').forEach(function (b) { b.classList.toggle('activo', b.dataset.f === formato); });
  }

  window.tbComprobanteAbrir = function (id) {
    venta = typeof id === 'object' ? id : buscarVenta(id);
    if (!venta) { avisar('No se encontró la venta.', 'error'); return; }
    pintarVista();
    openModal('modal-comprobante');
  };
  window.tbComprobanteFormato = function (f) {
    formato = f === 'a4' ? 'a4' : 't80';
    guardarLocal(CLAVE_FORMATO, formato);
    pintarVista();
  };
  window.tbComprobanteImprimir = function () {
    if (!venta) return;
    let raiz = $g('tb-print-root');
    if (!raiz) { raiz = document.createElement('div'); raiz.id = 'tb-print-root'; document.body.appendChild(raiz); }
    // el tamaño de la hoja depende del formato: el ticket es angosto y tan largo como haga falta
    let estilo = $g('tb-print-page');
    if (!estilo) { estilo = document.createElement('style'); estilo.id = 'tb-print-page'; document.head.appendChild(estilo); }
    estilo.textContent = formato === 'a4' ? '@page { size: A4; margin: 8mm; }' : '@page { size: 80mm auto; margin: 0; }';
    raiz.innerHTML = htmlComprobante(venta, formato);
    const limpiar = function () { raiz.innerHTML = ''; window.removeEventListener('afterprint', limpiar); };
    window.addEventListener('afterprint', limpiar);
    window.print();
  };

  // "Imprimir comprobante al cobrar": una casilla en cada ventana de venta
  window.tbComprobanteAlCobrar = function (marcado) { guardarLocal(CLAVE_AUTO, marcado ? '1' : '0'); document.querySelectorAll('.cmp-auto').forEach(c => { c.checked = marcado; }); };
  function iniciarCasillas() {
    const on = leerLocal(CLAVE_AUTO) === '1';
    document.querySelectorAll('.cmp-auto').forEach(function (c) { c.checked = on; });
  }
  // Lo llaman las ventanas de venta cuando Java devuelve la venta guardada
  window.tbComprobanteTrasVenta = function (resultado) {
    if (leerLocal(CLAVE_AUTO) !== '1') return;
    try { const v = JSON.parse(resultado); if (v && v.id) window.tbComprobanteAbrir(v); } catch (e) { /* sin comprobante */ }
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciarCasillas); else iniciarCasillas();
})();
