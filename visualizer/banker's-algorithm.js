// Global state
let state = {
    n: 5,
    m: 3,
    available: [],
    max: [],
    allocation: [],
    need: [],
    initialized: false,
    requestCount: 0
};

// Generate matrix input tables
function generateMatrices() {
    state.n = parseInt(document.getElementById('numProcesses').value);
    state.m = parseInt(document.getElementById('numResources').value);

    if (state.n < 1 || state.m < 1) {
        alert('Please enter valid positive numbers for processes and resources.');
        return;
    }

    const container = document.getElementById('matricesContainer');
    container.innerHTML = '';

    // Initialize arrays
    state.available = new Array(state.m).fill(0);
    state.max = Array(state.n).fill().map(() => new Array(state.m).fill(0));
    state.allocation = Array(state.n).fill().map(() => new Array(state.m).fill(0));

    // Available Vector
    container.innerHTML += '<div class="matrix-container"><h4 style="margin-top: 20px; color: #34495e;">Total Available Resources</h4>';
    let table = '<table class="matrix-table"><thead><tr><th>Resource</th>';
    for (let j = 0; j < state.m; j++) {
        table += `<th>R${j + 1}</th>`;
    }
    table += '</tr></thead><tbody><tr><td><strong>Available</strong></td>';
    for (let j = 0; j < state.m; j++) {
        table += `<td><input type="number" id="avail_${j}" value="0" min="0"></td>`;
    }
    table += '</tr></tbody></table></div>';
    container.innerHTML += table;

    // Max Matrix
    container.innerHTML += '<div class="matrix-container"><h4 style="margin-top: 20px; color: #34495e;">Max Demand Matrix</h4>';
    table = '<table class="matrix-table"><thead><tr><th>Process</th>';
    for (let j = 0; j < state.m; j++) {
        table += `<th>R${j + 1}</th>`;
    }
    table += '</tr></thead><tbody>';
    for (let i = 0; i < state.n; i++) {
        table += `<tr><td><strong>P${i + 1}</strong></td>`;
        for (let j = 0; j < state.m; j++) {
            table += `<td><input type="number" id="max_${i}_${j}" value="0" min="0"></td>`;
        }
        table += '</tr>';
    }
    table += '</tbody></table></div>';
    container.innerHTML += table;

    // Allocation Matrix
    container.innerHTML += '<div class="matrix-container"><h4 style="margin-top: 20px; color: #34495e;">Currently Allocated Matrix</h4>';
    table = '<table class="matrix-table"><thead><tr><th>Process</th>';
    for (let j = 0; j < state.m; j++) {
        table += `<th>R${j + 1}</th>`;
    }
    table += '</tr></thead><tbody>';
    for (let i = 0; i < state.n; i++) {
        table += `<tr><td><strong>P${i + 1}</strong></td>`;
        for (let j = 0; j < state.m; j++) {
            table += `<td><input type="number" id="alloc_${i}_${j}" value="0" min="0"></td>`;
        }
        table += '</tr>';
    }
    table += '</tbody></table></div>';
    container.innerHTML += table;

    // Update process dropdown
    const processSelect = document.getElementById('processSelect');
    processSelect.innerHTML = '<option value="">-- Select Process --</option>';
    for (let i = 0; i < state.n; i++) {
        processSelect.innerHTML += `<option value="${i}">P${i + 1}</option>`;
    }

    // Update request vector inputs
    const requestContainer = document.getElementById('requestVectorContainer');
    requestContainer.innerHTML = '<div style="display: flex; gap: 10px; flex-wrap: wrap;">';
    for (let j = 0; j < state.m; j++) {
        requestContainer.innerHTML += `
                    <div style="flex: 1; min-width: 80px;">
                        <label style="font-size: 0.9em; color: #7f8c8d;">R${j + 1}</label>
                        <input type="number" id="request_${j}" value="0" min="0" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                    </div>
                `;
    }
    requestContainer.innerHTML += '</div>';

    state.initialized = false;
}

// Read matrix values from inputs
function readMatrixValues() {
    // Read Available
    for (let j = 0; j < state.m; j++) {
        state.available[j] = parseInt(document.getElementById(`avail_${j}`).value) || 0;
    }

    // Read Max
    for (let i = 0; i < state.n; i++) {
        for (let j = 0; j < state.m; j++) {
            state.max[i][j] = parseInt(document.getElementById(`max_${i}_${j}`).value) || 0;
        }
    }

    // Read Allocation
    for (let i = 0; i < state.n; i++) {
        for (let j = 0; j < state.m; j++) {
            state.allocation[i][j] = parseInt(document.getElementById(`alloc_${i}_${j}`).value) || 0;
        }
    }
}

// Calculate Need Matrix
function calculateNeed() {
    state.need = Array(state.n).fill().map(() => new Array(state.m).fill(0));
    for (let i = 0; i < state.n; i++) {
        for (let j = 0; j < state.m; j++) {
            state.need[i][j] = state.max[i][j] - state.allocation[i][j];
        }
    }
}

// Display Need Matrix
function displayNeedMatrix() {
    const container = document.getElementById('needMatrixContainer');
    let table = '<table class="matrix-table readonly"><thead><tr><th>Process</th>';
    for (let j = 0; j < state.m; j++) {
        table += `<th>R${j + 1}</th>`;
    }
    table += '</tr></thead><tbody>';
    for (let i = 0; i < state.n; i++) {
        table += `<tr><td><strong>P${i + 1}</strong></td>`;
        for (let j = 0; j < state.m; j++) {
            table += `<td>${state.need[i][j]}</td>`;
        }
        table += '</tr>';
    }
    table += '</tbody></table>';
    container.innerHTML = table;
    document.getElementById('needMatrixSection').style.display = 'block';
}

// Safety Algorithm
function safetyAlgorithm() {
    const work = [...state.available];
    const finish = new Array(state.n).fill(false);
    const safeSequence = [];
    const steps = [];

    let count = 0;
    while (count < state.n) {
        let found = false;
        for (let i = 0; i < state.n; i++) {
            if (!finish[i]) {
                let canAllocate = true;
                for (let j = 0; j < state.m; j++) {
                    if (state.need[i][j] > work[j]) {
                        canAllocate = false;
                        break;
                    }
                }

                if (canAllocate) {
                    // Record step
                    steps.push({
                        step: count + 1,
                        process: i,
                        needCheck: true,
                        workBefore: [...work],
                        workAfter: work.map((w, j) => w + state.allocation[i][j])
                    });

                    // Update work
                    for (let j = 0; j < state.m; j++) {
                        work[j] += state.allocation[i][j];
                    }

                    finish[i] = true;
                    safeSequence.push(i);
                    count++;
                    found = true;
                }
            }
        }

        if (!found) {
            return { safe: false, sequence: [], steps: [] };
        }
    }

    return { safe: true, sequence: safeSequence, steps: steps };
}

// Display safety result
function displaySafetyResult(result) {
    const statusBox = document.getElementById('statusBox');

    if (result.safe) {
        statusBox.className = 'status-box status-safe';
        statusBox.textContent = 'SYSTEM STATUS: SAFE';

        const sequenceBox = document.getElementById('safeSequenceBox');
        const seqString = result.sequence.map(i => `P${i + 1}`).join(' → ');
        sequenceBox.innerHTML = `
                    <div class="sequence-box">
                        <strong>Safe Sequence Found:</strong><br>
                        <span>${seqString}</span>
                    </div>
                `;
        sequenceBox.style.display = 'block';

        // Display steps
        displaySteps(result.steps);
    } else {
        statusBox.className = 'status-box status-unsafe';
        statusBox.textContent = 'SYSTEM STATUS: UNSAFE (Deadlock may occur)';
        document.getElementById('safeSequenceBox').style.display = 'none';
        document.getElementById('stepsSection').style.display = 'none';
    }
}

// Display step-by-step simulation
function displaySteps(steps) {
    const container = document.getElementById('stepsTableContainer');
    let table = '<table class="steps-table"><thead><tr>';
    table += '<th>Step</th><th>Process</th><th>Need ≤ Work?</th><th>New Available (Work + Allocation)</th>';
    table += '</tr></thead><tbody>';

    for (const step of steps) {
        table += '<tr>';
        table += `<td>${step.step}</td>`;
        table += `<td>P${step.process + 1}</td>`;
        table += `<td class="check-yes">Yes</td>`;
        table += `<td>[${step.workAfter.join(', ')}]</td>`;
        table += '</tr>';
    }

    table += '</tbody></table>';
    container.innerHTML = table;
    document.getElementById('stepsSection').style.display = 'block';
}

// Run initial safety check
function runSafetyCheck() {
    if (!state.initialized && document.getElementById('matricesContainer').innerHTML === '') {
        alert('Please generate matrices first.');
        return;
    }

    readMatrixValues();
    calculateNeed();
    displayNeedMatrix();

    const result = safetyAlgorithm();
    displaySafetyResult(result);

    state.initialized = true;
}

// Submit resource request
function submitRequest() {
    if (!state.initialized) {
        alert('Please run the initial safety check first.');
        return;
    }

    const processIndex = parseInt(document.getElementById('processSelect').value);
    if (isNaN(processIndex)) {
        alert('Please select a process.');
        return;
    }

    const request = [];
    for (let j = 0; j < state.m; j++) {
        request[j] = parseInt(document.getElementById(`request_${j}`).value) || 0;
    }

    // Check if request is valid
    for (let j = 0; j < state.m; j++) {
        if (request[j] > state.need[processIndex][j]) {
            logRequest(processIndex, request, false, 'Request exceeds maximum need');
            return;
        }
        if (request[j] > state.available[j]) {
            logRequest(processIndex, request, false, 'Resources not available');
            return;
        }
    }

    // Try allocating temporarily
    const oldAvailable = [...state.available];
    const oldAllocation = state.allocation[processIndex].slice();

    for (let j = 0; j < state.m; j++) {
        state.available[j] -= request[j];
        state.allocation[processIndex][j] += request[j];
    }

    calculateNeed();
    const result = safetyAlgorithm();

    if (result.safe) {
        // Grant request
        logRequest(processIndex, request, true, `New state remains safe. Safe sequence: ${result.sequence.map(i => 'P' + (i + 1)).join(' → ')}`);
        displayNeedMatrix();
        displaySafetyResult(result);
    } else {
        // Deny request - rollback
        state.available = oldAvailable;
        state.allocation[processIndex] = oldAllocation;
        calculateNeed();
        logRequest(processIndex, request, false, 'Granting would lead to an unsafe state');
    }
}

// Log resource request
function logRequest(processIndex, request, granted, reason) {
    state.requestCount++;
    const logContainer = document.getElementById('requestLog');

    const logEntry = document.createElement('div');
    logEntry.className = `log-entry ${granted ? 'granted' : 'denied'}`;
    logEntry.innerHTML = `
                <div class="log-header">Request #${state.requestCount}</div>
                <div class="log-details">P${processIndex + 1} requests [${request.join(', ')}]</div>
                <div class="log-outcome">${granted ? '✓ GRANTED' : '✗ DENIED'}: ${reason}</div>
            `;

    logContainer.insertBefore(logEntry, logContainer.firstChild);
    document.getElementById('logSection').style.display = 'block';
}

// Toggle collapsible section
function toggleCollapsible() {
    const content = document.getElementById('stepsContent');
    const icon = document.getElementById('collapseIcon');

    if (content.classList.contains('active')) {
        content.classList.remove('active');
        icon.textContent = '▼';
    } else {
        content.classList.add('active');
        icon.textContent = '▲';
    }
}

// Initialize on page load
window.onload = function () {
    generateMatrices();
};