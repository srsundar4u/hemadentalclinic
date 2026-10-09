(() => {
    const input = document.getElementById('patientSearch');
    const patientId = document.getElementById('patientId');
    const options = document.getElementById('patientOptions');
    const status = document.getElementById('patientPickerStatus');
    if (!input || input.readOnly) return;

    const knownPatients = new Map();
    for (const option of options.options) knownPatients.set(option.value, option.dataset.id);
    let timer;
    let pending;
    let version = 0;

    function selectPatient() {
        patientId.value = knownPatients.get(input.value) || '';
        input.setCustomValidity(patientId.value ? '' : 'Select a patient from the search suggestions.');
    }

    async function search(query, requestVersion) {
        pending = new AbortController();
        status.textContent = 'Searching patients…';
        try {
            const url = new URL(input.dataset.optionsUrl, window.location.origin);
            url.searchParams.set('q', query);
            const response = await fetch(url, { signal: pending.signal, headers: { Accept: 'application/json' } });
            if (!response.ok || response.redirected) throw new Error('Search unavailable');
            const patients = await response.json();
            if (requestVersion !== version) return;
            options.replaceChildren();
            for (const patient of patients) {
                const label = `${patient.fullName} — ID ${patient.id} — ${patient.phone}`;
                knownPatients.set(label, String(patient.id));
                const option = document.createElement('option');
                option.value = label;
                options.append(option);
            }
            status.textContent = patients.length
                ? `${patients.length} matches. Select a suggestion; narrow your search if needed.`
                : 'No matching patients. Try another search or create a new patient.';
        } catch (error) {
            if (error.name === 'AbortError' || requestVersion !== version) return;
            status.textContent = 'Patient search is unavailable. Refresh the page and check that you are signed in.';
        }
    }

    function queueSearch() {
        clearTimeout(timer);
        if (pending) pending.abort();
        const requestVersion = ++version;
        selectPatient();
        if (patientId.value) return;
        const query = input.value.trim();
        timer = setTimeout(() => search(query, requestVersion), 250);
    }

    input.addEventListener('input', queueSearch);
    input.addEventListener('change', selectPatient);
    input.addEventListener('focus', () => { if (!input.value) queueSearch(); });
    input.form.addEventListener('submit', event => {
        selectPatient();
        if (!patientId.value) { event.preventDefault(); input.reportValidity(); }
    });
})();
