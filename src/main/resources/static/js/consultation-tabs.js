(() => {
    const tabs = Array.from(document.querySelectorAll('#visit-tabs [role="tab"]'));
    if (!tabs.length) return;
    document.getElementById('visit-tabs').hidden = false;
    function activate(tab) {
        for (const item of tabs) {
            const selected = item === tab;
            item.setAttribute('aria-selected', String(selected));
            item.tabIndex = selected ? 0 : -1;
            document.getElementById(item.getAttribute('aria-controls')).hidden = !selected;
        }
    }
    for (const tab of tabs) {
        tab.addEventListener('click', () => activate(tab));
        tab.addEventListener('keydown', event => {
            if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
            event.preventDefault();
            const next = event.key === 'Home' ? tabs[0] : event.key === 'End' ? tabs[tabs.length - 1] : tabs[(tabs.indexOf(tab) + 1) % tabs.length];
            activate(next); next.focus();
        });
    }
    const errorPanel = Array.from(document.querySelectorAll('[role="tabpanel"]')).find(panel => Array.from(panel.querySelectorAll('.field-error')).some(error => error.textContent.trim()));
    const initial = errorPanel ? tabs.find(tab => tab.getAttribute('aria-controls') === errorPanel.id) : tabs[0];
    activate(initial);
})();
