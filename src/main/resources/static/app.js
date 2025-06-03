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
    let renderedTabs = new Set();

    const API_BASE_URL = '/api/data';

    // Define standard proficiency levels for consistent ordering and coloring
    const ELA_PROFICIENCY_LEVELS_ORDERED = ["Below Proficient", "Approaching Proficient", "Proficient", "Highly Proficient", "N/A (Grade not in ELA 3-8)", "N/A (Score out of ELA range)"];
    const MATH_PROFICIENCY_LEVELS_ORDERED = ["Below Proficient", "Approaching Proficient", "Proficient", "Highly Proficient", "N/A (Math Grade not in 3-8)", "N/A (Score out of Math range)"];


    uploadButton.addEventListener('click', async () => {
        console.log("Upload button clicked."); // Log 0: Button click
        const file = csvFileInput.files[0];
        const year = yearInput.value;

        errorMessageDiv.textContent = '';
        errorMessageDiv.style.display = 'none';
        successMessageDiv.textContent = '';
        successMessageDiv.style.display = 'none';
        resultsSection.style.display = 'none';
        renderedTabs.clear();

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
            console.log("Attempting to fetch from API:", `${API_BASE_URL}/upload`); // Log 1
            const response = await fetch(`${API_BASE_URL}/upload`, {
                method: 'POST',
                body: formData
            });
            console.log("Fetch response received. Status:", response.status, "Ok:", response.ok, "StatusText:", response.statusText); // Log 2
            // Log response headers, especially Content-Type
            console.log("Response Headers:");
            response.headers.forEach((value, name) => {
                console.log(`  ${name}: ${value}`);
            });


            if (!response.ok) {
                console.error("Response not OK. Attempting to parse error JSON or text..."); // Log 3
                let errorText = `HTTP error! Status: ${response.status} ${response.statusText}.`;
                try {
                    const errorData = await response.json();
                    console.error("Error data from response (parsed as JSON):", errorData); // Log 5a
                    errorText = errorData.error || errorText;
                } catch (jsonError) {
                    console.error("Failed to parse error response as JSON:", jsonError); // Log 4
                    try {
                        const text = await response.text();
                        console.error("Error response as text:", text.substring(0, 500)); // Log 5b
                        errorText += ` Response body (text): ${text.substring(0,200)}...`;
                    } catch (textError) {
                        console.error("Failed to get error response as text:", textError); // Log 5c
                    }
                }
                throw new Error(errorText);
            }

            console.log("Response OK. Attempting to parse success JSON..."); // Log 6
            const data = await response.json(); // This line might fail if response is not JSON
            console.log("Successfully parsed JSON response:", data); // Log 7

            analysisDataStore = data;
            currentDatasetYear = data.datasetYear;

            console.log("Received data from backend (analysisDataStore):", analysisDataStore);
            console.log("Current dataset year set to:", currentDatasetYear); // Log 8


            if (data.message && !data.totalUnpivotedRecordsProcessed) {
                console.log("Displaying 'message' from backend:", data.message); // Log 9
                displaySuccess(data.message);
            } else if (data.totalUnpivotedRecordsProcessed > 0) {
                console.log("Processing successful data. Total records:", data.totalUnpivotedRecordsProcessed); // Log 10
                displaySuccess(`Successfully processed ${data.fileName} for year ${data.datasetYear}.`);
                setupTabsAndDisplayResults(data);
            } else {
                console.warn("No data processed, but no specific message. File might be empty or format issue."); // Log 11
                displayError("No data processed. The file might be empty or not in the expected format after the header.");
            }
        } catch (error) { // This catches errors from fetch, response.ok check, or response.json()
            console.error('Upload error (outer catch):', error.message, error.stack); // Log 12
            displayError(`Failed to process file: ${error.message}`);
        } finally {
            loadingMessage.style.display = 'none';
            uploadButton.disabled = false;
            console.log("Upload process finished (finally block)."); // Log 13
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

        Object.keys(chartInstances).forEach(destroyChart);
        renderedTabs.clear();

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

        if (!renderedTabs.has(tabId) && analysisDataStore) {
            renderChartsForTab(tabId, analysisDataStore);
            renderedTabs.add(tabId);
        }
    }

    function renderChartsForTab(tabId, data) {
        const panel = document.getElementById(`tab-${tabId}`);
        if (panel) {
            panel.querySelectorAll('canvas').forEach(canvas => {
                destroyChart(canvas.id);
            });
        }

        console.log(`Rendering charts for tab: ${tabId}`);

        switch (tabId) {
            case 'riseElaProficiency':
                renderRiseElaProficiencyDistributionChart(data.riseElaProficiencyDistributionByGradeByYear);
                break;
            case 'mathProficiency':
                renderMathProficiencyDistributionChart(data.mathProficiencyDistributionByGradeByYear);
                break;
            case 'subjectPerformance':
                renderSubjectPerformanceDistributionChart(data.subjectPerformanceLevelDistributionByYear);
                break;
            case 'overallYearlyMetrics':
                renderAverageOverallScaleScoreByYearChart(data.averageOverallScaleScoreByYear);
                renderOverallElaPassRateByYearChart(data.overallElaPassRateByYear);
                break;
            // 'subjectGroupScaleScores' tab and its charts are removed
            case 'specialEdComparison':
                populateSpecialEdSubjectFilter(data.averageOverallScaleScoreBySpecialEdAndSubjectAreaByYear);
                if (spEdSubjectFilter.options.length > 0) {
                    renderAvgOverallScaleScoreBySpecialEdChart(data.averageOverallScaleScoreBySpecialEdAndSubjectAreaByYear, spEdSubjectFilter.value);
                } else {
                    destroyChart('avgOverallScaleScoreBySpecialEdChart');
                    const ctx = document.getElementById('avgOverallScaleScoreBySpecialEdChart')?.getContext('2d');
                    if(ctx) ctx.fillText(`No data for Special Ed comparison for year ${currentDatasetYear}.`, 10, 50);
                }
                break;
            // 'demographics' tab and its charts are removed
        }
    }

    spEdSubjectFilter.addEventListener('change', () => {
        if (analysisDataStore && spEdSubjectFilter.value && renderedTabs.has('specialEdComparison')) {
            renderAvgOverallScaleScoreBySpecialEdChart(analysisDataStore.averageOverallScaleScoreBySpecialEdAndSubjectAreaByYear, spEdSubjectFilter.value);
        }
    });

    function getChartColors(count) {
        const colors = [];
        const palette = [
            'rgba(54, 162, 235, 0.7)', 'rgba(255, 99, 132, 0.7)', 'rgba(75, 192, 192, 0.7)',
            'rgba(255, 206, 86, 0.7)', 'rgba(153, 102, 255, 0.7)', 'rgba(255, 159, 64, 0.7)',
            'rgba(99, 255, 132, 0.7)', 'rgba(132, 99, 255, 0.7)'
        ];
        for (let i = 0; i < count; i++) {
            if (i < palette.length) {
                colors.push(palette[i]);
            } else if (typeof randomColor === 'function') {
                colors.push(randomColor({ luminosity: 'bright', format: 'rgba', alpha: 0.7 }));
            } else {
                const r = Math.floor(Math.random() * 200);
                const g = Math.floor(Math.random() * 200);
                const b = Math.floor(Math.random() * 200);
                colors.push(`rgba(${r},${g},${b},0.7)`);
            }
        }
        return colors;
    }

    function renderRiseElaProficiencyDistributionChart(dataByYear) {
        const chartId = 'riseElaProficiencyDistributionChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataByYear || !dataByYear[currentDatasetYear] || Object.keys(dataByYear[currentDatasetYear]).length === 0) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No RISE ELA proficiency distribution data for year ${currentDatasetYear}.`, 10, 50);
            return;
        }

        const yearData = dataByYear[currentDatasetYear];
        const grades = Object.keys(yearData).sort((a,b) => parseInt(a) - parseInt(b));

        const datasets = ELA_PROFICIENCY_LEVELS_ORDERED.map((level, index) => {
            const color = getChartColors(ELA_PROFICIENCY_LEVELS_ORDERED.length)[index];
            return {
                label: level,
                data: grades.map(grade => yearData[grade]?.[level] || 0),
                backgroundColor: color,
                borderColor: color.replace('0.7', '1'),
                borderWidth: 1
            };
        });

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: { labels: grades, datasets: datasets },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, title: { display: true, text: 'Number of Students' } },
                    x: { title: { display: true, text: 'Grade Level' } }
                },
                plugins: { legend: { position: 'top' }, title: { display: true, text: `RISE ELA Proficiency Distribution by Grade (${currentDatasetYear})` } }
            }
        });
    }

    function renderMathProficiencyDistributionChart(dataByYear) {
        const chartId = 'mathProficiencyDistributionChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataByYear || !dataByYear[currentDatasetYear] || Object.keys(dataByYear[currentDatasetYear]).length === 0) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No Math proficiency distribution data for year ${currentDatasetYear}.`, 10, 50);
            return;
        }

        const yearData = dataByYear[currentDatasetYear];
        const grades = Object.keys(yearData).sort((a,b) => parseInt(a) - parseInt(b));

        const datasets = MATH_PROFICIENCY_LEVELS_ORDERED.map((level, index) => {
            const color = getChartColors(MATH_PROFICIENCY_LEVELS_ORDERED.length)[index];
            return {
                label: level,
                data: grades.map(grade => yearData[grade]?.[level] || 0),
                backgroundColor: color,
                borderColor: color.replace('0.7', '1'),
                borderWidth: 1
            };
        });

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: { labels: grades, datasets: datasets },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, title: { display: true, text: 'Number of Students' } },
                    x: { title: { display: true, text: 'Grade Level' } }
                },
                plugins: { legend: { position: 'top' }, title: { display: true, text: `Math Proficiency Distribution by Grade (${currentDatasetYear})` } }
            }
        });
    }

    function renderSubjectPerformanceDistributionChart(dataByYear) {
        const chartId = 'subjectPerformanceDistributionChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataByYear || !dataByYear[currentDatasetYear] || Object.keys(dataByYear[currentDatasetYear]).length === 0) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No subject area performance distribution data for year ${currentDatasetYear}.`, 10, 50);
            return;
        }

        const yearData = dataByYear[currentDatasetYear];
        const subjectAreas = Object.keys(yearData);
        const allPerformanceLevels = [...new Set(subjectAreas.flatMap(sa => yearData[sa] ? Object.keys(yearData[sa]) : []))].sort();

        const datasets = allPerformanceLevels.map((level, index) => {
            const color = getChartColors(allPerformanceLevels.length)[index];
            return {
                label: level,
                data: subjectAreas.map(sa => yearData[sa]?.[level] || 0),
                backgroundColor: color,
                borderColor: color.replace('0.7', '1'),
                borderWidth: 1
            };
        });

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: { labels: subjectAreas, datasets: datasets },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, title: { display: true, text: 'Number of Students' } },
                    x: { title: { display: true, text: 'Subject Area (from CSV columns)' } }
                },
                plugins: { legend: { position: 'top' }, title: { display: true, text: `Distribution of Performance Levels by Subject Area (${currentDatasetYear})` } }
            }
        });
    }

    function renderAverageOverallScaleScoreByYearChart(dataByYear) {
        const chartId = 'avgOverallScaleScoreByYearChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataByYear || dataByYear[currentDatasetYear] === undefined || dataByYear[currentDatasetYear] === null) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No overall average scale score data for year ${currentDatasetYear}.`, 10, 50);
            return;
        }
        const avgScore = dataByYear[currentDatasetYear];

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: [`Year ${currentDatasetYear}`],
                datasets: [{
                    label: 'Average Overall Scale Score',
                    data: [avgScore],
                    backgroundColor: [getChartColors(1)[0]],
                    borderColor: [getChartColors(1)[0].replace('0.7','1')],
                    borderWidth: 1,
                    barPercentage: 0.4
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: { y: { beginAtZero: false, title: { display: true, text: 'Average Scale Score' } } },
                plugins: { legend: { display: false }, title: { display: true, text: `Average Overall Scale Score (${currentDatasetYear})` } }
            }
        });
    }

    function renderOverallElaPassRateByYearChart(dataByYear) {
        const chartId = 'overallElaPassRateByYearChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataByYear || dataByYear[currentDatasetYear] === undefined || dataByYear[currentDatasetYear] === null) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No overall ELA pass rate data for year ${currentDatasetYear}.`, 10, 50);
            return;
        }
        const passRate = dataByYear[currentDatasetYear];
        const notPassingRate = Math.max(0, 100 - passRate);

        chartInstances[chartId] = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Passing (RISE ELA Proficient/Highly Proficient) (%)', 'Not Passing (%)'],
                datasets: [{
                    label: 'Overall ELA Pass Rate (RISE)',
                    data: [passRate, notPassingRate],
                    backgroundColor: [getChartColors(2)[0], getChartColors(2)[1]],
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { position: 'top' }, title: { display: true, text: `Overall ELA Pass Rate (Based on RISE ELA Proficiency - ${currentDatasetYear})` } }
            }
        });
    }

    function populateSpecialEdSubjectFilter(dataBySpEd) {
        spEdSubjectFilter.innerHTML = '';
        if (!dataBySpEd || Object.keys(dataBySpEd).length === 0 || !currentDatasetYear) return;

        const subjectsForYear = new Set();
        ['true', 'false'].forEach(isSpEdKey => {
            if (dataBySpEd[isSpEdKey] && dataBySpEd[isSpEdKey][currentDatasetYear]) {
                Object.keys(dataBySpEd[isSpEdKey][currentDatasetYear]).forEach(subject => subjectsForYear.add(subject));
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

    function renderAvgOverallScaleScoreBySpecialEdChart(dataBySpEd, selectedSubject) {
        const chartId = 'avgOverallScaleScoreBySpecialEdChart';
        destroyChart(chartId);
        const ctx = document.getElementById(chartId)?.getContext('2d');
        if(!ctx) return;

        if (!dataBySpEd || !currentDatasetYear || !selectedSubject) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText('Select a subject area to view Special Ed comparison.', 10, 50);
            return;
        }

        const avgScoreNonSpecialEd = dataBySpEd['false']?.[currentDatasetYear]?.[selectedSubject] || 0;
        const avgScoreSpecialEd = dataBySpEd['true']?.[currentDatasetYear]?.[selectedSubject] || 0;

        if (avgScoreNonSpecialEd === 0 && avgScoreSpecialEd === 0 &&
            !(dataBySpEd['false']?.[currentDatasetYear]?.[selectedSubject] || dataBySpEd['true']?.[currentDatasetYear]?.[selectedSubject])) {
            ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
            ctx.fillText(`No data for ${selectedSubject} in ${currentDatasetYear} to compare Special Ed status.`, 10, 50);
            return;
        }

        chartInstances[chartId] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Non-Special Ed', 'Special Ed'],
                datasets: [{
                    label: `Avg. Overall Scale Score - ${selectedSubject} (${currentDatasetYear})`,
                    data: [avgScoreNonSpecialEd, avgScoreSpecialEd],
                    backgroundColor: [getChartColors(2)[0], getChartColors(2)[1]], borderWidth: 1
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: { y: { beginAtZero: false, title: { display: true, text: 'Average Overall Scale Score' } } },
                plugins: { legend: { display: false }, title: { display: true, text: `Avg. Overall Scale Score by Sp.Ed Status for ${selectedSubject} (${currentDatasetYear})` } }
            }
        });
    }

});