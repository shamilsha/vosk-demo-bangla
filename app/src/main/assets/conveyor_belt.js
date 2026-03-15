/**
 * Reusable conveyor belt component.
 * Use createWordBelt(viewportId, trayId, opts) to create a belt.
 * Call belt.animateMoveToNext() to smoothly scroll by one row (same behavior everywhere).
 */
(function(global) {
    var ROW_HEIGHT_DEFAULT = 40;
    var ANIM_MS_DEFAULT = 480;
    var COLORS = ['#1565c0', '#ef6c00', '#2e7d32', '#c2185b', '#f9a825', '#6a1b9a', '#00838f', '#d84315', '#558b2f', '#ad1457'];

    function createWordBelt(viewportId, trayId, opts) {
        opts = opts || {};
        var viewport = document.getElementById(viewportId);
        var tray = document.getElementById(trayId);
        if (!viewport || !tray) return null;

        var visibleCount = opts.visibleCount || 7;
        var rowHeight = opts.rowHeight || ROW_HEIGHT_DEFAULT;
        var animMs = opts.animMs != null ? opts.animMs : ANIM_MS_DEFAULT;
        var centerIndex = opts.centerIndex !== undefined ? opts.centerIndex : 3;
        var centerTrayIndex = 1 + centerIndex;
        var totalRows = visibleCount + 1;
        var colors = opts.colors || COLORS;
        var data = opts.data || [];
        var currentIndex = 0;
        var syncSlave = null;

        function n() { return data.length; }
        function mod(i) {
            var s = n();
            if (s === 0) return 0;
            return ((i % s) + s) % s;
        }
        function textAt(offset) {
            if (n() === 0) return '';
            return data[mod(currentIndex + offset)] || '';
        }
        var createItemFn = opts.createItem || null;
        function createItem(text, dataIndexForColor) {
            if (createItemFn) return createItemFn(text, dataIndexForColor, rowHeight);
            var div = document.createElement('div');
            div.className = 'conveyor-item';
            div.style.height = rowHeight + 'px';
            var ci = (dataIndexForColor % colors.length + colors.length) % colors.length;
            var inner = document.createElement('div');
            inner.className = 'conveyor-item-inner';
            inner.style.background = colors[ci];
            inner.style.color = '#fff';
            inner.textContent = text;
            div.appendChild(inner);
            return div;
        }
        function buildTray() {
            tray.innerHTML = '';
            if (n() === 0) return;
            for (var i = 0; i < totalRows; i++) {
                var offset = centerTrayIndex - i;
                var text = textAt(offset);
                if (offset === 0 && !text && n() > 0) text = data[0];
                var dataIdx = mod(currentIndex + offset);
                tray.appendChild(createItem(text, dataIdx));
            }
        }
        function updateItemTexts() {
            var items = tray.querySelectorAll('.conveyor-item');
            for (var i = 0; i < items.length; i++) {
                var offset = centerTrayIndex - i;
                var text = textAt(offset);
                if (offset === 0 && !text && n() > 0) text = data[0];
                var inner = items[i].querySelector('.conveyor-item-inner');
                if (inner) inner.textContent = text;
            }
        }
        function updateFocusItemText(text) {
            var items = tray.querySelectorAll('.conveyor-item');
            for (var i = 0; i < items.length; i++) {
                var offset = centerTrayIndex - i;
                if (offset !== 0) continue;
                var inner = items[i].querySelector('.conveyor-item-inner');
                if (inner) { inner.textContent = text; break; }
            }
        }
        function setData(arr) {
            if (!Array.isArray(arr) || arr.length === 0) return;
            data = arr;
            currentIndex = 0;
            buildTray();
            if (opts.onFocusChange) opts.onFocusChange();
        }
        function setCurrentIndex(i) {
            if (n() === 0) return;
            currentIndex = mod(i);
            buildTray();
            if (opts.onFocusChange) opts.onFocusChange();
        }
        function getCurrentIndex() { return currentIndex; }
        function getFocusText() {
            var t = textAt(0);
            return (t !== '' && t != null) ? t : (n() > 0 ? data[0] : '');
        }
        function getCount() { return n(); }

        /** Smoothly move belt down by one row. Use this everywhere for consistent "move to next" behavior. */
        function animateMoveToNext() {
            if (tray.classList.contains('animate') || n() === 0) return;
            tray.style.transform = 'translateY(' + rowHeight + 'px)';
            tray.style.transition = 'transform ' + (animMs / 1000) + 's cubic-bezier(0.35, 0.05, 0.55, 0.95)';
            tray.classList.add('animate');
            setTimeout(function() {
                tray.classList.remove('animate');
                tray.style.transform = '';
                tray.style.transition = '';
                tray.removeChild(tray.lastElementChild);
                currentIndex = mod(currentIndex + 1);
                var newItem = createItem(textAt(centerTrayIndex), mod(currentIndex + centerTrayIndex));
                tray.insertBefore(newItem, tray.firstChild);
                updateItemTexts();
                /* Do not sync slave here: when both belts call animateMoveToNext(), each updates its own index; syncing here would make the slave advance twice. Sync only on drag (applySnap). */
                if (opts.onFocusChange) opts.onFocusChange();
            }, animMs);
        }

        var dragStartY = 0, dragStartTrayY = 0, isDragging = false;
        var maxSnapRows = 3;
        function getTrayTranslateY() {
            var t = tray.style.transform;
            if (!t || t === '') return 0;
            var m = t.match(/translateY\(([-\d.]+)px\)/);
            return m ? parseFloat(m[1]) : 0;
        }
        function setTrayTranslateY(px) {
            tray.style.transform = px === 0 ? '' : 'translateY(' + px + 'px)';
        }
        function applySnap(deltaY) {
            if (n() === 0) return;
            var k = Math.round(deltaY / rowHeight);
            k = Math.max(-maxSnapRows, Math.min(maxSnapRows, k));
            if (k === 0) { setTrayTranslateY(0); if (syncSlave) syncSlave.setTrayTranslateY(0); return; }
            currentIndex = mod(currentIndex + k);
            buildTray();
            setTrayTranslateY(0);
            if (syncSlave) { syncSlave.setCurrentIndex(currentIndex); syncSlave.setTrayTranslateY(0); }
            if (opts.onFocusChange) opts.onFocusChange();
        }
        function onPointerStart(clientY) {
            isDragging = true;
            dragStartY = clientY;
            dragStartTrayY = getTrayTranslateY();
            tray.style.transition = 'none';
        }
        function onPointerMove(clientY) {
            if (!isDragging) return;
            var ty = dragStartTrayY + (clientY - dragStartY);
            setTrayTranslateY(ty);
            if (syncSlave && syncSlave.setTrayTranslateY) syncSlave.setTrayTranslateY(ty);
        }
        function onPointerEnd(clientY) {
            if (!isDragging) return;
            isDragging = false;
            tray.style.transition = '';
            applySnap(dragStartTrayY + (clientY - dragStartY));
        }
        var interactive = opts.interactive !== false;
        if (interactive) {
            viewport.addEventListener('touchstart', function(e) {
                e.preventDefault();
                if (e.touches.length === 1) onPointerStart(e.touches[0].clientY);
            }, { passive: false });
            viewport.addEventListener('touchmove', function(e) {
                if (e.touches.length === 1) { e.preventDefault(); onPointerMove(e.touches[0].clientY); }
            }, { passive: false });
            viewport.addEventListener('touchend', function(e) {
                if (e.changedTouches.length === 1) onPointerEnd(e.changedTouches[0].clientY);
            });
            viewport.addEventListener('mousedown', function(e) {
                e.preventDefault();
                onPointerStart(e.clientY);
                var up = function(ev) {
                    document.removeEventListener('mousemove', move);
                    document.removeEventListener('mouseup', up);
                    onPointerEnd(ev.clientY);
                };
                var move = function(ev) { onPointerMove(ev.clientY); };
                document.addEventListener('mousemove', move);
                document.addEventListener('mouseup', up);
            });
        }
        if (opts.syncSlave) syncSlave = opts.syncSlave;
        buildTray();
        return {
            setData: setData,
            animateMoveToNext: animateMoveToNext,
            moveDown: animateMoveToNext,
            getFocusText: getFocusText,
            getCurrentIndex: getCurrentIndex,
            setCurrentIndex: setCurrentIndex,
            getCount: getCount,
            getTrayTranslateY: getTrayTranslateY,
            setTrayTranslateY: setTrayTranslateY,
            updateFocusItemText: updateFocusItemText
        };
    }

    global.CONVEYOR_ROW_HEIGHT = ROW_HEIGHT_DEFAULT;
    global.CONVEYOR_ANIM_MS = ANIM_MS_DEFAULT;
    global.CONVEYOR_COLORS = COLORS;
    global.createWordBelt = createWordBelt;
})(typeof window !== 'undefined' ? window : this);
