document.addEventListener('DOMContentLoaded', () => {
    const csvFileInput = document.getElementById('csvFile');
    const yearInput = document.getElementById('dataYear');
    const uploadButton = document.getElementById('uploadButton');
    const resultsSection = document.getElementById('results-section');
    const loadingMessage = document.getElementById('loadingMessage');
    const errorMessageDiv = document.getElementById('errorMessage');
    const successMessageDiv = document.getElementById('successMessage');

    const resultsYearSpan = document.getElementById('resultsYear');
    const resultsFileNameSpan = document.getElementById('resultsFileName');
    const totalRecordsSpan = document.getElementById('totalRecords');
    const spEdSubjectFilter = document.getElementById('spEdSubjectFilter');

    const tabButtonsContainer = document.querySelector('.tab-navigation');
    const tabPanels = document.querySelectorAll('.tab-panel');

    let analysisDataStore = null;
    let currentDatasetYear = null;
    let chartInstances = {};
    let renderedTabs = new Set(); // Keep track of tabs whose charts have been rendered

    const API_BASE_URL = '/api/data';

    uploadButton.addEventListener('click', async () => {
        const file = csvFileInput.files[0];
        const year = yearInput.value;

        errorMessageDiv.textContent = '';
        errorMessageDiv.style.display = 'none';
        successMessageDiv.textContent = '';
        successMessageDiv.style.display = 'none';
        resultsSection.style.display = 'none';
        renderedTabs.clear(); // Clear rendered tabs on new upload

        if (!file) {
            displayError("Please select a CSV file.");
            return;
        }
        if (!year || isNaN(parseInt(year)) || parseInt(year) < 1900 || parseInt(year) > 2100) {
            displayError("Please enter a valid academic year (e.g., 2023).");
            return;
        }

        loadingMessage.style.display = 'flex';
        uploadButton.disabled = true;

        const formData = new FormData();
        formData.append('file', file);
        formData.append('year', year);

        try {
            const response = await fetch(`${API_BASE_URL}/upload`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ error: `HTTP error! Status: ${response.status}. No error details provided.` }));
                throw new Error(errorData.error || `HTTP error! Status: ${response.status}`);
            }

            const data = await response.json();
            analysisDataStore = data;
            currentDatasetYear = data.datasetYear;

            if (data.message && !data.totalUnpivotedRecordsProcessed) {
                displaySuccess(data.message);
            } else if (data.totalUnpivotedRecordsProcessed > 0) {
                displaySuccess(`Successfully processed ${data.fileName} for year ${data.datasetYear}.`);
                setupTabsAndDisplayResults(data);
            } else {
                displayError("No data processed. The file might be empty or not in the expected format after the header.");
            }
        } catch (error) {
            console.error('Upload error:', error);
            displayError(`Failed to process file: ${error.message}`);
        } finally {
            loadingMessage.style.display = 'none';
            uploadButton.disabled = false;
        }
    });

    function displayError(message) {
        errorMessageDiv.textContent = message;
        errorMessageDiv.style.display = 'block';
        successMessageDiv.style.display = 'none';
    }
    function displaySuccess(message) {
        successMessageDiv.textContent = message;
        successMessageDiv.style.display = 'block';
        errorMessageDiv.style.display = 'none';
    }

    function destroyChart(chartId) {
        if (chartInstances[chartId]) {
            chartInstances[chartId].destroy();
            delete chartInstances[chartId];
        }
    }

    function setupTabsAndDisplayResults(data) {
        resultsSection.style.display = 'block';
        resultsYearSpan.textContent = data.datasetYear || 'N/A';
        resultsFileNameSpan.textContent = data.fileName || 'N/A';
        totalRecordsSpan.textContent = data.totalUnpivotedRecordsProcessed || '0';

        // Destroy all existing charts before setting up new tabs
        Object.keys(chartInstances).forEach(destroyChart);
        renderedTabs.clear();

        // Activate the first tab by default
        const firstTabButton = tabButtonsContainer.querySelector('.tab-button');
        if (firstTabButton) {
            activateTab(firstTabButton.dataset.tab);
        }
    }

    tabButtonsContainer.addEventListener('click', (event) => {
        if (event.target.matches('.tab-button')) {
            const tabId = event.target.dataset.tab;
            activateTab(tabId);
        }
    });

    function activateTab(tabId) {
        tabButtonsContainer.querySelectorAll('.tab-button').forEach(button => {
            button.classList.toggle('active', button.dataset.tab === tabId);
        });
        tabPanels.forEach(panel => {
            panel.classList.toggle('active', panel.id === `tab-${tabId}`);
        });

        // Render charts for the activated tab if not already rendered
        if (!renderedTabs.has(tabId) && analysisDataStore) {
            renderChartsForTab(tabId, analysisDataStore);
            renderedTabs.add(tabId);
        }
    }

    function renderChartsForTab(tabId, data) {
        // Destroy existing charts in this tab before rendering (important if data updates)
        // This logic might need refinement if tabs can be re-rendered with new data without full upload
        const panel = document.getElementById(`tab-${tabId}`);
        if (panel) {
            panel.querySelectorAll('canvas').forEach(canvas => {
                destroyChart(canvas.id);
            });
        }

        switch (tabId) {
            case 'performanceDistribution':
                renderPerformanceDistributionChart(data.performanceLevelDistributionBySubjectByYear);
                break;
            case 'avgScaleScores':
                renderAverageScaleScoreBySubjectChart(data.averageScaleScoreBySubjectByYear);
                break;
            case 'passRates':
                renderPassRateBySubjectChart(data.passRatesBySubjectByYear);
                break;
            case 'specialEdComparison':
                populateSpecialEdSubjectFilter(data.averageScaleScoreBySpecialEdAndSubjectByYear);
                if (spEdSubjectFilter.options.length > 0) {
                    renderAvgScaleScoreBySpecialEdChart(data.averageScaleScoreBySpecialEdAndSubjectByYear, spEdSubjectFilter.value);
                } else {
                    destroyChart('avgScaleScoreBySpecialEdChart'); // Ensure canvas is cleared if no filter options
                    const ctx = document.getElementById('avgScaleScoreBySpecialEdChart')?.getContext('2d');
                    if(ctx) ctx.fillText(`No data for Special Ed comparison for year ${currentDatasetYear}.`, 10, 50);
                }
                break;
            case 'demographics':
                renderDemographicChart(data.averageScaleScoreByEthnicityByYear, 'avgScoreByEthnicityChart', 'Average Scale Score by Ethnicity');
                renderDemographicChart(data.averageScaleScoreByGenderByYear, 'avgScoreByGenderChart', 'Average Scale Score by Gender');
                renderDemographicChart(data.averageScaleScoreByGradeLevelByYear, 'avgScoreByGradeLevelChart', 'Average Scale Score by Grade Level');
                renderDemographicChart(data.averageScaleScoreByOverallPerformanceByYear, 'avgScoreByOverallPerformanceChart', 'Average Scale Score by Overall Performance');
                break;
        }
    }

    spEdSubjectFilter.addEventListener('change', () => {
        if (analysisDataStore && spEdSubjectFilter.value && renderedTabs.has('specialEdComparison')) { // Re-render only if tab is active
            renderAvgScaleScoreBySpecialEdChart(analysisDataStore.averageScaleScoreBySpecialEdAndSubjectByYear, spEdSubjectFilter.value);
        }
    });

    // --- Chart Rendering Functions (Mostly unchanged, but now called selectively) ---

    function getChartColors(count) {
        const colors = [];
        for (let i = 0; i < count; i++) {
            colors.push(randomColor({ luminosity: 'bright', format: 'rgba', alpha: 0.7 }));
        }
        return colors;
    }

    function renderPerformanceDistributionChart(dataByYear) {
        const chartId = 'performanceDistributionChart';
        destroyChart(chartId); // Ensure previous instance is destroyed
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;


        if (!dataByYear || !dataByYear[currentDatasetYear] || Object.keys(dataByYear[currentDatasetYear]).length === 0) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No performance distribution data for year ${currentDatasetYear}.`, 10, 50);
            return;
        }

        const yearData = dataByYear[currentDatasetYear];
        const subjects = Object.keys(yearData);
        const performanceLevels = [...new Set(subjects.flatMap(subject => Object.keys(yearData[subject])))].sort();

        const datasets = performanceLevels.map((level, index) => {
            const color = getChartColors(performanceLevels.length)[index];
            return {
                label: level,
                data: subjects.map(subject => yearData[subject][level] || 0),
                backgroundColor: color,
                borderColor: color.replace('0.7', '1'),
                borderWidth: 1
            };
        });

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: { labels: subjects, datasets: datasets },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, title: { display: true, text: 'Number of Students' } },
                    x: { title: { display: true, text: 'Subject / Assessment Area' } }
                },
                plugins: { legend: { position: 'top' }, title: { display: true, text: `Performance Level Distribution (${currentDatasetYear})` } }
            }
        });
    }

    function renderAverageScaleScoreBySubjectChart(dataByYear) {
        const chartId = 'avgScaleScoreBySubjectChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataByYear || !dataByYear[currentDatasetYear] || Object.keys(dataByYear[currentDatasetYear]).length === 0) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No average scale score data by subject for year ${currentDatasetYear}.`, 10, 50);
            return;
        }
        const yearData = dataByYear[currentDatasetYear];
        const subjects = Object.keys(yearData);
        const scores = subjects.map(subject => yearData[subject]);
        const colors = getChartColors(subjects.length);

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: subjects,
                datasets: [{
                    label: `Average Scale Score (${currentDatasetYear})`, data: scores,
                    backgroundColor: colors, borderColor: colors.map(c => c.replace('0.7', '1')), borderWidth: 1
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false, indexAxis: 'y',
                scales: { x: { beginAtZero: false, title: { display: true, text: 'Average Scale Score' } } },
                plugins: { legend: { display: false }, title: { display: true, text: `Average Scale Score by Subject (${currentDatasetYear})` } }
            }
        });
    }

    function renderPassRateBySubjectChart(dataByYear) {
        const chartId = 'passRateBySubjectChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataByYear || !dataByYear[currentDatasetYear] || Object.keys(dataByYear[currentDatasetYear]).length === 0) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No pass rate data by subject for year ${currentDatasetYear}.`, 10, 50);
            return;
        }
        const yearData = dataByYear[currentDatasetYear];
        const subjects = Object.keys(yearData);
        const rates = subjects.map(subject => yearData[subject]);
        const colors = getChartColors(subjects.length);

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: subjects,
                datasets: [{
                    label: `Pass Rate (%) (${currentDatasetYear})`, data: rates,
                    backgroundColor: colors, borderColor: colors.map(c => c.replace('0.7', '1')), borderWidth: 1
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: { y: { beginAtZero: true, max: 100, title: { display: true, text: 'Pass Rate (%)' } } },
                plugins: { legend: { display: false }, title: { display: true, text: `Pass Rate by Subject (${currentDatasetYear})` } }
            }
        });
    }

    function populateSpecialEdSubjectFilter(dataBySpEd) {
        spEdSubjectFilter.innerHTML = '';
        if (!dataBySpEd || Object.keys(dataBySpEd).length === 0 || !currentDatasetYear) return;

        const subjectsForYear = new Set();
        [true, false].forEach(isSpEd => {
            if (dataBySpEd[isSpEd] && dataBySpEd[isSpEd][currentDatasetYear]) {
                Object.keys(dataBySpEd[isSpEd][currentDatasetYear]).forEach(subject => subjectsForYear.add(subject));
            }
        });

        const sortedSubjects = [...subjectsForYear].sort();
        sortedSubjects.forEach(subject => {
            const option = document.createElement('option');
            option.value = subject;
            option.textContent = subject;
            spEdSubjectFilter.appendChild(option);
        });
    }

    function renderAvgScaleScoreBySpecialEdChart(dataBySpEd, selectedSubject) {
        const chartId = 'avgScaleScoreBySpecialEdChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataBySpEd || !currentDatasetYear || !selectedSubject) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText('Select a subject to view Special Ed comparison.', 10, 50);
            return;
        }

        const avgScoreNonSpecialEd = dataBySpEd[false]?.[currentDatasetYear]?.[selectedSubject] || 0;
        const avgScoreSpecialEd = dataBySpEd[true]?.[currentDatasetYear]?.[selectedSubject] || 0;

        if (avgScoreNonSpecialEd === 0 && avgScoreSpecialEd === 0 && !(dataBySpEd[false]?.[currentDatasetYear]?.[selectedSubject] || dataBySpEd[true]?.[currentDatasetYear]?.[selectedSubject])) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No data for ${selectedSubject} in ${currentDatasetYear} to compare Special Ed status.`, 10, 50);
            return;
        }

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Non-Special Ed', 'Special Ed'],
                datasets: [{
                    label: `Avg Scale Score - ${selectedSubject} (${currentDatasetYear})`,
                    data: [avgScoreNonSpecialEd, avgScoreSpecialEd],
                    backgroundColor: [getChartColors(2)[0], getChartColors(2)[1]], borderWidth: 1
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: { y: { beginAtZero: false, title: { display: true, text: 'Average Scale Score' } } },
                plugins: { legend: { display: false }, title: { display: true, text: `Avg Scale Score by Special Ed Status for ${selectedSubject} (${currentDatasetYear})` } }
            }
        });
    }

    function renderDemographicChart(dataByYear, chartElementId, chartTitlePrefix) {
        destroyChart(chartElementId);
        const ctx = document.getElementById(chartElementId)?.getContext('2d');
        if(!ctx) return;

        if (!dataByYear || !dataByYear[currentDatasetYear] || Object.keys(dataByYear[currentDatasetYear]).length === 0) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No data for ${chartTitlePrefix} for year ${currentDatasetYear}.`, 10, 50);
            return;
        }

        const yearData = dataByYear[currentDatasetYear];
        const categories = Object.keys(yearData);
        const scores = categories.map(cat => yearData[cat]);
        const colors = getChartColors(categories.length);

        chartInstances[chartElementId] = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: categories,
                datasets: [{ label: `Average Scale Score`, data: scores, backgroundColor: colors, hoverOffset: 4 }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { position: 'top' }, title: { display: true, text: `${chartTitlePrefix} (${currentDatasetYear})` } }
            }
        });
    }
});
