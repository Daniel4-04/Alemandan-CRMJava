document.addEventListener('DOMContentLoaded', function() {
    // Elementos
    const btnIniciarCompra = document.getElementById('btnIniciarCompra');
    const inputBuscarProducto = document.getElementById('inputBuscarProducto');
    const btnBuscarManual = document.getElementById('btnBuscarManual');
    const resultadosBusqueda = document.getElementById('resultadosBusqueda');
    const totalPagar = document.getElementById('totalPagar');
    const pagoBtns = Array.from(document.querySelectorAll('.pago-btn'));
    const btnFinalizarCompra = document.getElementById('btnFinalizarCompra');
    const ticketProductos = document.getElementById('ticketProductos');
    const ticketEmpty = document.getElementById('ticketEmpty');
    const ticketSubtotal = document.getElementById('ticketSubtotal');
    const ticketIva = document.getElementById('ticketIva');
    const ticketTotal = document.getElementById('ticketTotal');
    const ventaExitosa = document.getElementById('ventaExitosa');
    const ventaError = document.getElementById('ventaError');
    const ticketFecha = document.getElementById('ticketFecha');
    const ticketNumVenta = document.getElementById('ticketNumVenta');

    // Estado de la venta
    let venta = {
        productos: [],
        metodoPago: null
    };
    let productosCache = []; // Para búsqueda manual

    function resetVenta() {
        venta = {
            productos: [],
            metodoPago: null
        };
        ticketNumVenta.textContent = "N° Venta: ----";
        ticketProductos.innerHTML = '';
        ticketProductos.appendChild(ticketEmpty);
        ticketSubtotal.textContent = "$0";
        ticketIva.textContent = "$0";
        ticketTotal.textContent = "$0";
        totalPagar.textContent = "$0";
        pagoBtns.forEach(btn => btn.classList.remove('selected'));
        btnFinalizarCompra.classList.remove('enabled');
        btnFinalizarCompra.disabled = true;
        ventaExitosa.style.display = "none";
        ventaError.style.display = "none";
    }

    btnIniciarCompra.addEventListener('click', function() {
        resetVenta();
        inputBuscarProducto.disabled = false;
        btnBuscarManual.disabled = false;
        pagoBtns.forEach(btn => btn.disabled = false);
        btnIniciarCompra.disabled = true;
        ticketFecha.textContent = "Fecha: " + new Date().toLocaleDateString();
    });

    function formatMoney(n) {
        return '$' + Number(n || 0).toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function renderTicket() {
        // Si no hay productos, muestra vacío
        if (venta.productos.length === 0) {
            ticketProductos.innerHTML = '';
            ticketProductos.appendChild(ticketEmpty);
        } else {
            ticketProductos.innerHTML = '';
            venta.productos.forEach((prod, idx) => {
                const lineSubtotal = (Number(prod.precio) || 0) * (Number(prod.cantidad) || 0);
                const lineIva = lineSubtotal * ((Number(prod.iva) || 0) / 100);
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${prod.nombre}</td>
                    <td>${formatMoney(prod.precio)}</td>
                    <td><input type="number" min="1" value="${prod.cantidad}" style="width:55px;"
                        data-index="${idx}" class="ticket-cantidad-input"></td>
                    <td>${formatMoney(lineSubtotal + lineIva)}</td>
                    <td><button class="ticket-remove-btn" data-index="${idx}"><i class="fas fa-trash"></i></button></td>
                `;
                ticketProductos.appendChild(tr);
            });
        }
        // Subtotal, IVA, Total (calculo por producto usando su iva individual)
        let subtotal = venta.productos.reduce((acc, prod) => acc + (Number(prod.precio) || 0) * (Number(prod.cantidad) || 0), 0);
        let iva = venta.productos.reduce((acc, prod) => acc + ((Number(prod.precio) || 0) * (Number(prod.cantidad) || 0)) * ((Number(prod.iva) || 0) / 100), 0);
        let total = subtotal + iva;
        ticketSubtotal.textContent = formatMoney(subtotal);
        ticketIva.textContent = formatMoney(iva);
        ticketTotal.textContent = formatMoney(total);
        totalPagar.textContent = formatMoney(total);
        // Habilitar finalizar si hay productos y método
        if (venta.productos.length > 0 && venta.metodoPago) {
            btnFinalizarCompra.classList.add('enabled');
            btnFinalizarCompra.disabled = false;
        } else {
            btnFinalizarCompra.classList.remove('enabled');
            btnFinalizarCompra.disabled = true;
        }
    }

    // Buscar producto por nombre/id (AJAX)
    inputBuscarProducto.addEventListener('input', function() {
        const term = this.value.trim();
        if (term.length < 1) {
            resultadosBusqueda.innerHTML = '';
            resultadosBusqueda.style.display = 'none';
            return;
        }
        axios.get(`/api/productos/buscar?term=${encodeURIComponent(term)}`)
            .then(res => {
                productosCache = res.data;
                if (!Array.isArray(productosCache) || productosCache.length === 0) {
                    resultadosBusqueda.innerHTML = '<div class="resultado-producto">No se encontraron productos</div>';
                    resultadosBusqueda.style.display = '';
                } else {
                    resultadosBusqueda.innerHTML = productosCache.map(prod => `
                        <div class="resultado-producto" data-id="${prod.id}">
                            <b>${prod.nombre}</b> <span style="color:#3971f7;">(Stock: ${prod.cantidad})</span>
                            <span>${formatMoney(prod.precio)}</span>
                            <small style="margin-left:8px;color:#666">${(prod.iva || 0)}%</small>
                        </div>
                    `).join('');
                    resultadosBusqueda.style.display = '';
                }
            })
            .catch(() => {
                resultadosBusqueda.innerHTML = '<div class="resultado-producto">Error al buscar productos</div>';
                resultadosBusqueda.style.display = '';
            });
    });

    // Buscar manualmente (mostrar todos los productos)
    btnBuscarManual.addEventListener('click', function() {
        axios.get(`/api/productos/buscar?term=`)
            .then(res => {
                productosCache = res.data;
                if (!Array.isArray(productosCache) || productosCache.length === 0) {
                    resultadosBusqueda.innerHTML = '<div class="resultado-producto">No se encontraron productos</div>';
                    resultadosBusqueda.style.display = '';
                } else {
                    resultadosBusqueda.innerHTML = productosCache.map(prod => `
                        <div class="resultado-producto" data-id="${prod.id}">
                            <b>${prod.nombre}</b> <span style="color:#3971f7;">(Stock: ${prod.cantidad})</span>
                            <span>${formatMoney(prod.precio)}</span>
                            <small style="margin-left:8px;color:#666">${(prod.iva || 0)}%</small>
                        </div>
                    `).join('');
                    resultadosBusqueda.style.display = '';
                }
            })
            .catch(() => {
                resultadosBusqueda.innerHTML = '<div class="resultado-producto">Error al listar productos</div>';
                resultadosBusqueda.style.display = '';
            });
    });

    // Click en producto encontrado
    resultadosBusqueda.addEventListener('click', function(e) {
        const prodDiv = e.target.closest('.resultado-producto');
        if (!prodDiv) return;
        const prodId = prodDiv.getAttribute('data-id');
        const prod = productosCache.find(p => p.id == prodId);
        if (!prod) return;
        // Ver si ya está en ticket
        if (venta.productos.some(p => p.id == prod.id)) {
            ventaError.textContent = "El producto ya fue agregado al ticket";
            ventaError.style.display = "";
            setTimeout(() => ventaError.style.display = "none", 2000);
            return;
        }
        // Validar stock
        if ((prod.cantidad || 0) < 1) {
            ventaError.textContent = "No hay suficiente stock de este producto";
            ventaError.style.display = "";
            setTimeout(() => ventaError.style.display = "none", 2000);
            return;
        }
        // Asegurar que iva esté definido (0 si es null) y precio
        const ivaVal = prod.iva == null ? 0 : Number(prod.iva);
        const precioVal = prod.precio == null ? 0 : Number(prod.precio);
        venta.productos.push({ id: prod.id, nombre: prod.nombre, cantidad: 1, iva: ivaVal, precio: precioVal });
        resultadosBusqueda.innerHTML = '';
        resultadosBusqueda.style.display = 'none';
        inputBuscarProducto.value = '';
        renderTicket();
    });

    // Cambiar cantidad en ticket
    ticketProductos.addEventListener('input', function(e) {
        if (e.target.classList.contains('ticket-cantidad-input')) {
            const idx = e.target.getAttribute('data-index');
            let qty = parseInt(e.target.value, 10);
            if (isNaN(qty) || qty < 1) qty = 1;
            // Validar stock con AJAX
            const prod = venta.productos[idx];
            axios.get(`/api/productos/buscar?term=${encodeURIComponent(prod.nombre)}`)
                .then(res => {
                    const prodActual = res.data.find(p => p.id == prod.id);
                    if (prodActual && qty > prodActual.cantidad) {
                        ventaError.textContent = `No hay suficiente stock para "${prod.nombre}". Disponible: ${prodActual.cantidad}`;
                        ventaError.style.display = "";
                        setTimeout(() => ventaError.style.display = "none", 2000);
                        qty = prodActual.cantidad;
                        e.target.value = qty;
                    }
                    venta.productos[idx].cantidad = qty;
                    renderTicket();
                })
                .catch(() => {
                    // Si falla la validación remota, aplicar el cambio localmente
                    venta.productos[idx].cantidad = qty;
                    renderTicket();
                });
        }
    });

    // Eliminar producto del ticket
    ticketProductos.addEventListener('click', function(e) {
        if (e.target.closest('.ticket-remove-btn')) {
            const idx = e.target.closest('.ticket-remove-btn').getAttribute('data-index');
            venta.productos.splice(idx, 1);
            renderTicket();
        }
    });

    // Selección método de pago
    pagoBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            pagoBtns.forEach(b => b.classList.remove('selected'));
            this.classList.add('selected');
            venta.metodoPago = this.getAttribute('data-metodo');
            renderTicket();
        });
    });

    // Finalizar compra AJAX (ahora abre recibo PDF si backend lo devuelve en base64)
    btnFinalizarCompra.addEventListener('click', function() {
        btnFinalizarCompra.disabled = true;
        btnFinalizarCompra.classList.remove('enabled');
        ventaExitosa.style.display = "none";
        ventaError.style.display = "none";

        // Prepara datos
        const ventaPayload = {
            detalles: venta.productos.map(p => ({
                producto: { id: p.id },
                cantidad: p.cantidad
            })),
            metodoPago: venta.metodoPago
        };
        console.log("Enviando venta:", ventaPayload);

        axios.post('/ventas/api/ventas/registrar', ventaPayload)
            .then(res => {
                if (res.data.success) {
                    // Si el backend devolvió el PDF en base64, abrir en nueva pestaña
                    if (res.data.receiptBase64) {
                        try {
                            const pdfBase64 = res.data.receiptBase64;
                            const linkSource = "data:application/pdf;base64," + pdfBase64;
                            const downloadLink = document.createElement("a");
                            downloadLink.href = linkSource;
                            const ventaId = res.data.ventaId ? res.data.ventaId.toString().padStart(4, "0") : "recibo";
                            downloadLink.download = "recibo_venta_" + ventaId + ".pdf";
                            downloadLink.target = "_blank";
                            document.body.appendChild(downloadLink);
                            downloadLink.click();
                            document.body.removeChild(downloadLink);
                        } catch (err) {
                            console.warn("No se pudo abrir el recibo en PDF automáticamente:", err);
                        }
                    }

                    ventaExitosa.textContent = "¡Venta realizada exitosamente!";
                    ventaExitosa.style.display = "block";
                    ticketNumVenta.textContent = "N° Venta: " + res.data.ventaId.toString().padStart(4, "0");
                    btnIniciarCompra.disabled = false;
                    inputBuscarProducto.disabled = true;
                    btnBuscarManual.disabled = true;
                    pagoBtns.forEach(btn => btn.disabled = true);
                    btnFinalizarCompra.disabled = true;
                    setTimeout(() => {
                        ventaExitosa.style.display = "none";
                        resetVenta();
                        btnIniciarCompra.disabled = false;
                        inputBuscarProducto.disabled = true;
                        btnBuscarManual.disabled = true;
                        pagoBtns.forEach(btn => btn.disabled = true);
                    }, 2500);
                } else {
                    ventaError.textContent = res.data.error || "Error inesperado";
                    ventaError.style.display = "";
                    btnFinalizarCompra.disabled = false;
                    btnFinalizarCompra.classList.add('enabled');
                }
            })
            .catch(err => {
                ventaError.textContent = err.response?.data?.error || "Error de comunicación";
                ventaError.style.display = "";
                btnFinalizarCompra.disabled = false;
                btnFinalizarCompra.classList.add('enabled');
            });
    });

    // Inicialmente deshabilitado
    resetVenta();
    inputBuscarProducto.disabled = true;
    btnBuscarManual.disabled = true;
    pagoBtns.forEach(btn => btn.disabled = true);
    btnIniciarCompra.disabled = false;
});