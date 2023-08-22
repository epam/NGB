/* global Miew:false */
(function () {
  const viewer = new Miew({
    container: document.getElementsByClassName('miew-container')[0],
    load: '1CRN',
  });

  if (viewer.init()) {
    viewer.run();
  }

  document.getElementById('load-4xn6').addEventListener('click', function () {
    viewer.load('mmtf:4XN6');
  });

  document.getElementById('load-1crn').addEventListener('click', function () {
    viewer.load('pdb:1CRN');
  });

  document.getElementById('repAdd').addEventListener('click', function () {
    viewer._extractRepresentation();
  });

  document.getElementById('aoChange').addEventListener('change', function () {
    viewer.set('ao', this.checked);
  });

  const msg = document.getElementById('messages');
  const liStile = 'list-style-type: none; margin: 6px; text-align: center; padding: 10px; border-radius: 5px; color: #ffffff;';

  function infoEventHandler(eventName, style, timout) {
    const li = document.createElement('li');
    li.appendChild(document.createTextNode(eventName));
    li.setAttribute('style', style);
    msg.appendChild(li);

    setTimeout(() => { msg.removeChild(msg.childNodes[0]); }, timout);
  }

  viewer.addEventListener('fetchingDone', () => {
    infoEventHandler('Fetching finished', liStile + 'background-color:#59575a', 5000);
  });

  viewer.addEventListener('parsingDone', () => {
    infoEventHandler('Parsing finished', liStile + 'background-color:#59575a', 5000);
  });

  viewer.addEventListener('repAdded', () => {
    infoEventHandler('New rep added', liStile + 'background-color:#30575a', 5000);
  });

  viewer.settings.addEventListener('change:ao', () => {
    infoEventHandler('AO toggled ', liStile + 'background-color:#39376a', 5000);
  });

  const EVENT_TYPE = {
    NONE: -1, ROTATE: 0, TRANSLATE: 1, SCALE: 2,
  };

  const bar = document.getElementById('velocity');
  const event = document.getElementById('event');

  let lastEvent = EVENT_TYPE.NONE;
  let count = 0;
  let timeOuts = [];

  function transformEventHandler(eventType, eventName) {
    if (lastEvent !== eventType) {
      if (timeOuts) {
        timeOuts.forEach((item) => {
          clearTimeout(item);
        });
        timeOuts = [];
      }
      count = 1;
      timeOuts.push(setTimeout(() => { count--; }, 1000));
      lastEvent = eventType;
      event.innerHTML = eventName;
    } else {
      count++;
      timeOuts.push(setTimeout(() => { count--; }, 1000));
    }
  }

  viewer.addEventListener('rotate', () => {
    transformEventHandler(EVENT_TYPE.ROTATE, 'Rotate');
  });

  viewer.addEventListener('translatePivot', () => {
    transformEventHandler(EVENT_TYPE.TRANSLATE, 'Translate');
  });

  viewer.addEventListener('zoom', () => {
    transformEventHandler(EVENT_TYPE.SCALE, 'Scale');
  });

  setInterval(() => {
    bar.style.height = count + '%';
    bar.style.bottom = (count - 100) + '%';
  }, 10);
}());
