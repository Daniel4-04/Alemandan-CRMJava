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

    function renderTicket() {
        // Si no hay productos, muestra vacío
        if (venta.productos.length === 0) {
            ticketProductos.innerHTML = '';
            ticketProductos.appendChild(ticketEmpty);
        } else {
            ticketProductos.innerHTML = '';
            venta.productos.forEach((prod, idx) => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${prod.nombre}</td>
                    <td>$${prod.precio.toFixed(2)}</td>
                    <td><input type="number" min="1" value="${prod.cantidad}" style="width:55px;"
                        data-index="${idx}" class="ticket-cantidad-input"></td>
                    <td>$${(prod.precio * prod.cantidad).toFixed(2)}</td>
                    <td><button class="ticket-remove-btn" data-index="${idx}"><i class="fas fa-trash"></i></button></td>
                `;
                ticketProductos.appendChild(tr);
            });
        }
        // Subtotal, IVA, Total
        let subtotal = venta.productos.reduce((acc, prod) => acc + prod.precio * prod.cantidad, 0);
        let iva = subtotal * 0.19;
        let total = subtotal + iva;
        ticketSubtotal.textContent = `$${subtotal.toFixed(2)}`;
        ticketIva.textContent = `$${iva.toFixed(2)}`;
        ticketTotal.textContent = `$${total.toFixed(2)}`;
        totalPagar.textContent = `$${total.toFixed(2)}`;
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
                if (productosCache.length === 0) {
                    resultadosBusqueda.innerHTML = '<div class="resultado-producto">No se encontraron productos</div>';
                    resultadosBusqueda.style.display = '';
                } else {
                    resultadosBusqueda.innerHTML = productosCache.map(prod => `
                        <div class="resultado-producto" data-id="${prod.id}">
                            <b>${prod.nombre}</b> <span style="color:#3971f7;">(Stock: ${prod.cantidad})</span> <span>$${prod.precio.toFixed(2)}</span>
                        </div>
                    `).join('');
                    resultadosBusqueda.style.display = '';
                }
            });
    });

    // Buscar manualmente (mostrar todos los productos)
    btnBuscarManual.addEventListener('click', function() {
        axios.get(`/api/productos/buscar?term=`)
            .then(res => {
                productosCache = res.data;
                if (productosCache.length === 0) {
                    resultadosBusqueda.innerHTML = '<div class="resultado-producto">No se encontraron productos</div>';
                    resultadosBusqueda.style.display = '';
                } else {
                    resultadosBusqueda.innerHTML = productosCache.map(prod => `
                        <div class="resultado-producto" data-id="${prod.id}">
                            <b>${prod.nombre}</b> <span style="color:#3971f7;">(Stock: ${prod.cantidad})</span> <span>$${prod.precio.toFixed(2)}</span>
                        </div>
                    `).join('');
                    resultadosBusqueda.style.display = '';
                }
            });
    });

    // Click en producto encontrado
    resultadosBusqueda.addEventListener('click', function(e) {
        const prodDiv = e.target.closest('.resultado-producto');
        if (!prodDiv) return;
        const prodId = prodDiv.getAttribute('data-id');
        const prod = productosCache.find(p => p.id == prodId);
        // Ver si ya está en ticket
        if (venta.productos.some(p => p.id == prod.id)) {
            ventaError.textContent = "El producto ya fue agregado al ticket";
            ventaError.style.display = "";
            setTimeout(() => ventaError.style.display = "none", 2000);
            return;
        }
        // Validar stock
        if (prod.cantidad < 1) {
            ventaError.textContent = "No hay suficiente stock de este producto";
            ventaError.style.display = "";
            setTimeout(() => ventaError.style.display = "none", 2000);
            return;
        }
        venta.productos.push({ ...prod, cantidad: 1 });
        resultadosBusqueda.innerHTML = '';
        resultadosBusqueda.style.display = 'none';
        inputBuscarProducto.value = '';
        renderTicket();
    });

    // Cambiar cantidad en ticket
    ticketProductos.addEventListener('input', function(e) {
        if (e.target.classList.contains('ticket-cantidad-input')) {
            const idx = e.target.getAttribute('data-index');
            let qty = parseInt(e.target.value);
            if (isNaN(qty) || qty < 1) qty = 1;
            // Validar stock con AJAX
            const prod = venta.productos[idx];
            axios.get(`/api/productos/buscar?term=${encodeURIComponent(prod.nombre)}`)
                .then(res => {
                    const prodActual = res.data.find(p => p.id == prod.id);
                    if (qty > prodActual.cantidad) {
                        ventaError.textContent = `No hay suficiente stock para "${prod.nombre}". Disponible: ${prodActual.cantidad}`;
                        ventaError.style.display = "";
                        setTimeout(() => ventaError.style.display = "none", 2000);
                        qty = prodActual.cantidad;
                        e.target.value = qty;
                    }
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

    // Finalizar compra AJAX
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
        axios.post('/api/ventas/registrar', ventaPayload)
            .then(res => {
                if (res.data.success) {
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