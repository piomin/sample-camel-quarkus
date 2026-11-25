(() => {
  'use strict';

  const LEVEL_COLORS = ['var(--lvl-0)', 'var(--lvl-1)', 'var(--lvl-2)', 'var(--lvl-3)'];

  const WEATHER_CODES = {
    0: ['Clear sky', '☀️'], 1: ['Mainly clear', '🌤️'], 2: ['Partly cloudy', '⛅'], 3: ['Overcast', '☁️'],
    45: ['Fog', '🌫️'], 48: ['Rime fog', '🌫️'],
    51: ['Light drizzle', '🌦️'], 53: ['Drizzle', '🌦️'], 55: ['Dense drizzle', '🌧️'],
    56: ['Freezing drizzle', '🌧️'], 57: ['Dense freezing drizzle', '🌧️'],
    61: ['Light rain', '🌦️'], 63: ['Rain', '🌧️'], 65: ['Heavy rain', '🌧️'],
    66: ['Freezing rain', '🌧️'], 67: ['Heavy freezing rain', '🌧️'],
    71: ['Light snow', '🌨️'], 73: ['Snow', '🌨️'], 75: ['Heavy snow', '❄️'], 77: ['Snow grains', '❄️'],
    80: ['Light showers', '🌦️'], 81: ['Showers', '🌧️'], 82: ['Violent showers', '⛈️'],
    85: ['Snow showers', '🌨️'], 86: ['Heavy snow showers', '❄️'],
    95: ['Thunderstorm', '⛈️'], 96: ['Thunderstorm with hail', '⛈️'], 99: ['Severe thunderstorm with hail', '⛈️']
  };

  const form = document.getElementById('rating-form');
  const submit = document.getElementById('submit');
  const errorBox = document.getElementById('error');
  const resultBox = document.getElementById('result');

  // Level thresholds mirror MeteoService scoring: 0 = great (green) ... 3 = poor (red).
  const byThresholds = (value, limits) => {
    const idx = limits.findIndex(limit => value <= limit);
    return idx === -1 ? 3 : idx;
  };

  const airQualityLevel = aqi => byThresholds(aqi, [25, 50, 80]);
  const windLevel = wind => (wind < 12 ? 0 : wind < 20 ? 1 : wind < 30 ? 2 : 3);
  const precipLevel = p => (p === 0 ? 0 : p <= 0.1 ? 1 : p <= 2 ? 2 : 3);
  const temperatureLevel = t => (t >= 8 && t <= 18 ? 0 : t >= 5 && t <= 22 ? 1 : t >= 2 && t <= 27 ? 2 : 3);

  const sunLevel = (temp, cloud) => {
    if (temp > 32 && cloud < 40) return 3;
    if (temp > 28 && cloud < 25) return 2;
    if (temp > 25 && cloud < 20) return 1;
    return 0;
  };

  const weatherLevel = code => {
    if (code >= 95) return 3;
    if (code >= 45 && code <= 48) return 2;
    if (code >= 51) return 1;
    return 0;
  };

  const gradeLevel = grade => ({ A: 0, B: 1, C: 2 }[grade.charAt(0)] ?? 3);

  const aqiNote = aqi => ['Fresh, clean air', 'Fair air quality', 'Poor air quality', 'Unhealthy air'][airQualityLevel(aqi)];
  const tempNote = t => (t < 8 ? 'A bit cold' : t > 18 ? 'A bit warm' : 'Ideal for running') + (temperatureLevel(t) >= 2 ? ' - dress accordingly' : '');
  const windNote = w => ['Calm to light breeze', 'Noticeable wind', 'Strong wind', 'Dangerous wind'][windLevel(w)];
  const precipNote = p => ['Dry', 'Barely any rain', 'Wet conditions', 'Heavy precipitation'][precipLevel(p)];
  const sunNote = (t, c) => ['No sun or heat stress', 'Warm and sunny', 'Hot and sunny - hydrate', 'Heat and strong sun - avoid'][sunLevel(t, c)];

  const buildCards = r => {
    const [weatherText, weatherIcon] = WEATHER_CODES[r.weatherCode] ?? ['Unknown', '❔'];
    return [
      { title: 'Air quality', icon: '🍃', value: r.airQualityIndex, unit: 'EAQI', note: aqiNote(r.airQualityIndex),
        level: airQualityLevel(r.airQualityIndex), fill: r.airQualityIndex / 100 },
      { title: 'Feels like', icon: '🌡️', value: r.apparentTemperature.toFixed(1), unit: '°C',
        note: `${tempNote(r.apparentTemperature)} (actual ${r.temperature.toFixed(1)} °C)`,
        level: temperatureLevel(r.apparentTemperature), fill: (r.apparentTemperature + 10) / 50 },
      { title: 'Wind', icon: '💨', value: r.windSpeed.toFixed(1), unit: 'km/h', note: windNote(r.windSpeed),
        level: windLevel(r.windSpeed), fill: r.windSpeed / 60 },
      { title: 'Precipitation', icon: '☔', value: r.precipitation.toFixed(1), unit: 'mm', note: precipNote(r.precipitation),
        level: precipLevel(r.precipitation), fill: r.precipitation / 5 },
      { title: 'Sky', icon: weatherIcon, value: weatherText, unit: '', note: `Weather code ${r.weatherCode}`,
        level: weatherLevel(r.weatherCode), fill: weatherLevel(r.weatherCode) / 3 + 0.1 },
      { title: 'Cloud cover', icon: '☁️', value: r.cloudCover, unit: '%', note: sunNote(r.temperature, r.cloudCover),
        level: sunLevel(r.temperature, r.cloudCover), fill: r.cloudCover / 100 }
    ];
  };

  const el = (tag, className, text) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  };

  const renderCard = (card, index) => {
    const root = el('article', `card lvl-${card.level}`);
    root.style.setProperty('--i', index);

    const head = el('div', 'card-head');
    head.append(el('span', 'card-icon', card.icon), el('span', null, card.title));

    const value = el('div', 'card-value', String(card.value));
    if (card.unit) value.append(el('small', null, card.unit));

    const meter = el('div', 'meter');
    const bar = el('i');
    meter.append(bar);
    const pct = Math.round(Math.min(1, Math.max(0.04, card.fill)) * 100);
    requestAnimationFrame(() => requestAnimationFrame(() => { bar.style.width = `${pct}%`; }));

    root.append(head, value, el('p', 'card-note', card.note), meter);
    return root;
  };

  const render = r => {
    const grade = document.getElementById('grade');
    grade.textContent = r.rating;
    grade.style.setProperty('--c', LEVEL_COLORS[gradeLevel(r.rating)]);

    document.getElementById('place').textContent = `${r.city}, ${r.country.toUpperCase()}`;
    document.getElementById('verdict').textContent = r.description;
    document.getElementById('coords').textContent = `${r.latitude.toFixed(3)}, ${r.longitude.toFixed(3)}`;

    const cards = document.getElementById('cards');
    cards.replaceChildren(...buildCards(r).map(renderCard));
    resultBox.hidden = false;
    document.body.classList.add('has-result');
  };

  const showError = message => {
    errorBox.textContent = message;
    errorBox.hidden = false;
    resultBox.hidden = true;
    document.body.classList.remove('has-result');
  };

  const errorMessage = async response => {
    const body = await response.text().catch(() => '');
    if (/city not found/i.test(body)) return 'City not found. Check the city name and country code.';
    return `Something went wrong (HTTP ${response.status}). Please try again.`;
  };

  form.addEventListener('submit', async event => {
    event.preventDefault();
    errorBox.hidden = true;
    submit.disabled = true;
    submit.classList.add('loading');

    try {
      const response = await fetch('/rating', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({
          city: form.city.value.trim(),
          country: form.country.value.trim().toUpperCase()
        })
      });
      if (!response.ok) {
        showError(await errorMessage(response));
        return;
      }
      // An unknown city makes the route fail, but the response is still 200 with an empty body
      const text = await response.text();
      if (!text.trim()) {
        showError('City not found. Check the city name and country code.');
        return;
      }
      render(JSON.parse(text));
    } catch (e) {
      showError('Cannot reach the server. Please try again.');
    } finally {
      submit.disabled = false;
      submit.classList.remove('loading');
    }
  });
})();
