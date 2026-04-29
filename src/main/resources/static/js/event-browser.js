(function () {
  const BUSINESS_TIME_ZONE = "Europe/Sofia";
  const PLACEHOLDER_TONES = ["tone-1", "tone-2", "tone-3", "tone-4", "tone-5", "tone-6"];
  const BUSINESS_PARTS_FORMATTER = new Intl.DateTimeFormat("en-GB", {
    timeZone: BUSINESS_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  });
  const MONTH_LABEL_FORMATTER = new Intl.DateTimeFormat("bg-BG", {
    timeZone: BUSINESS_TIME_ZONE,
    month: "long",
    year: "numeric",
  });
  const TIME_FORMATTER = new Intl.DateTimeFormat("en-GB", {
    timeZone: BUSINESS_TIME_ZONE,
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  });

  function init(root) {
    const scope = root instanceof Element ? root : document;
    const browsers = [];

    initEventPlaceholders(scope);
    initEventPageInputs(scope);

    if (scope.matches && scope.matches("[data-event-browser]")) {
      browsers.push(scope);
    }

    scope.querySelectorAll?.("[data-event-browser]").forEach((browser) => {
      if (!browsers.includes(browser)) {
        browsers.push(browser);
      }
    });

    browsers.forEach(initBrowser);
  }

  function initEventPlaceholders(scope) {
    const mediaBlocks = [];

    if (scope.matches && scope.matches(".event-card__media:not(.has-image)")) {
      mediaBlocks.push(scope);
    }

    scope.querySelectorAll?.(".event-card__media:not(.has-image)").forEach((media) => {
      if (!mediaBlocks.includes(media)) {
        mediaBlocks.push(media);
      }
    });

    mediaBlocks.forEach((media) => {
      if (!(media instanceof HTMLElement) || media.dataset.eventPlaceholderReady === "true") {
        return;
      }

      const tone = PLACEHOLDER_TONES[Math.floor(Math.random() * PLACEHOLDER_TONES.length)];
      media.classList.add(tone);
      media.dataset.eventPlaceholderReady = "true";
    });
  }

  function initEventPageInputs(scope) {
    const forms = [];

    if (scope.matches && scope.matches("[data-event-page-input]")) {
      forms.push(scope);
    }

    scope.querySelectorAll?.("[data-event-page-input]").forEach((form) => {
      if (!forms.includes(form)) {
        forms.push(form);
      }
    });

    forms.forEach((form) => {
      if (!(form instanceof HTMLFormElement) || form.dataset.eventPageInputReady === "true") {
        return;
      }

      form.dataset.eventPageInputReady = "true";
      const pageInput = form.querySelector("input[name='pageDisplay']");
      if (!(pageInput instanceof HTMLInputElement)) {
        return;
      }

      const hiddenPage =
        form.querySelector("input[name='page']") ||
        Object.assign(document.createElement("input"), {
          type: "hidden",
          name: "page",
        });

      if (!hiddenPage.parentNode) {
        form.appendChild(hiddenPage);
      }

      const normalizePage = () => {
        const raw = Number.parseInt(pageInput.value, 10);
        const max = Number.parseInt(pageInput.max || "1", 10);
        const safeValue = Number.isNaN(raw) ? 1 : Math.min(Math.max(raw, 1), max);
        hiddenPage.value = String(safeValue - 1);
      };

      pageInput.addEventListener("change", () => {
        normalizePage();
        if (form.requestSubmit) {
          form.requestSubmit();
        } else {
          form.submit();
        }
      });

      form.addEventListener("submit", () => {
        normalizePage();
      });
    });
  }

  function initBrowser(browser) {
    if (!(browser instanceof HTMLElement) || browser.dataset.eventBrowserReady === "true") {
      return;
    }

    browser.dataset.eventBrowserReady = "true";

    const panes = Array.from(browser.querySelectorAll("[data-event-pane]"));
    const buttons = Array.from(browser.querySelectorAll("[data-event-view-btn]"));
    const defaultView = browser.dataset.defaultView === "calendar" ? "calendar" : "list";
    const viewInputs = Array.from(
      (browser.closest("main") || browser).querySelectorAll("input[name='view']")
    );

    const setView = (view) => {
      panes.forEach((pane) => {
        const active = pane.dataset.eventPane === view;
        pane.hidden = !active;
        pane.classList.toggle("is-active", active);
      });

      buttons.forEach((button) => {
        const active = button.dataset.eventViewBtn === view;
        button.classList.toggle("is-active", active);
        button.setAttribute("aria-pressed", String(active));
      });

      viewInputs.forEach((input) => {
        input.value = view;
      });
    };

    buttons.forEach((button) => {
      button.addEventListener("click", (event) => {
        event.preventDefault();
        setView(button.dataset.eventViewBtn === "calendar" ? "calendar" : "list");
      });
    });

    browser.querySelectorAll("[data-event-calendar]").forEach((calendar) => {
      initCalendar(calendar);
    });

    setView(defaultView);
  }

  function initCalendar(container) {
    const seeds = Array.from(container.querySelectorAll("[data-event-calendar-seed]"));
    const events = seeds
      .map((seed) => {
        const start = seed.dataset.start ? new Date(seed.dataset.start) : null;
        const startParts = start instanceof Date && !Number.isNaN(start.getTime())
          ? getBusinessDateParts(start)
          : null;

        return {
          title: seed.dataset.title || "Събитие",
          club: seed.dataset.club || "",
          location: seed.dataset.location || "",
          href: seed.dataset.href || "#",
          registrationClosed: seed.dataset.registrationClosed === "true",
          start,
          startParts,
        };
      })
      .filter((event) => event.start instanceof Date && !Number.isNaN(event.start.getTime()) && event.startParts !== null)
      .sort((a, b) => a.start - b.start);

    const today = getBusinessDateParts(new Date());
    const baseMonth = events.length > 0
      ? { year: events[0].startParts.year, month: events[0].startParts.month }
      : { year: today.year, month: today.month };
    const state = {
      currentMonth: { ...baseMonth },
      minMonth:
        events.length > 0 ? { year: events[0].startParts.year, month: events[0].startParts.month } : null,
      maxMonth:
        events.length > 0
          ? {
            year: events[events.length - 1].startParts.year,
            month: events[events.length - 1].startParts.month,
          }
          : null,
      events,
    };

    const shell = document.createElement("div");
    shell.className = "event-calendar-shell";
    container.appendChild(shell);

    const header = document.createElement("div");
    header.className = "event-calendar__header";

    const prev = document.createElement("button");
    prev.type = "button";
    prev.className = "event-calendar__nav";
    prev.textContent = "Предишен";

    const label = document.createElement("p");
    label.className = "event-calendar__label";

    const next = document.createElement("button");
    next.type = "button";
    next.className = "event-calendar__nav";
    next.textContent = "Следващ";

    header.appendChild(prev);
    header.appendChild(label);
    header.appendChild(next);

    const weekdays = document.createElement("div");
    weekdays.className = "event-calendar__weekdays";
    ["Пон", "Вто", "Сря", "Чет", "Пет", "Съб", "Нед"].forEach((day) => {
      const cell = document.createElement("span");
      cell.textContent = day;
      weekdays.appendChild(cell);
    });

    const grid = document.createElement("div");
    grid.className = "event-calendar__grid";

    const empty = document.createElement("div");
    empty.className = "event-calendar__empty";
    empty.textContent = "Няма планирани събития за този месец.";

    shell.appendChild(header);
    shell.appendChild(weekdays);
    shell.appendChild(grid);
    shell.appendChild(empty);

    prev.addEventListener("click", () => {
      state.currentMonth = shiftMonth(state.currentMonth, -1);
      renderCalendar(state, label, grid, empty, prev, next);
    });

    next.addEventListener("click", () => {
      state.currentMonth = shiftMonth(state.currentMonth, 1);
      renderCalendar(state, label, grid, empty, prev, next);
    });

    renderCalendar(state, label, grid, empty, prev, next);
  }

  function renderCalendar(state, label, grid, empty, prev, next) {
    grid.innerHTML = "";

    const monthStart = state.currentMonth;
    label.textContent = formatMonthLabel(monthStart.year, monthStart.month);

    if (state.minMonth) {
      prev.disabled = isSameMonthOrBefore(state.currentMonth, state.minMonth);
    } else {
      prev.disabled = true;
    }
    if (state.maxMonth) {
      next.disabled = isSameMonthOrAfter(state.currentMonth, state.maxMonth);
    } else {
      next.disabled = true;
    }

    const monthEvents = state.events.filter((event) => isSameMonth(event.startParts, monthStart));
    empty.hidden = monthEvents.length > 0;

    const firstDayOffset = getFirstDayOffset(monthStart.year, monthStart.month);
    const totalDays = getDaysInMonth(monthStart.year, monthStart.month);

    for (let i = 0; i < firstDayOffset; i += 1) {
      const filler = document.createElement("div");
      filler.className = "event-calendar__day event-calendar__day--filler";
      grid.appendChild(filler);
    }

    for (let day = 1; day <= totalDays; day += 1) {
      const dayCell = document.createElement("div");
      dayCell.className = "event-calendar__day";

      const number = document.createElement("span");
      number.className = "event-calendar__date";
      number.textContent = String(day);
      dayCell.appendChild(number);

      monthEvents
        .filter((event) => event.startParts.day === day)
        .slice(0, 3)
        .forEach((event) => {
          const link = document.createElement("a");
          link.className = "event-calendar__chip";
          if (event.registrationClosed) {
            link.classList.add("event-calendar__chip--closed");
          }
          link.href = event.href;
          link.textContent = `${formatTime(event.start)} ${event.title}`;
          link.title = `${event.title}${event.club ? " - " + event.club : ""}`;
          dayCell.appendChild(link);
        });

      const hiddenCount = monthEvents.filter((event) => event.startParts.day === day).length - 3;
      if (hiddenCount > 0) {
        const more = document.createElement("span");
        more.className = "event-calendar__more";
        more.textContent = `+${hiddenCount} още`;
        dayCell.appendChild(more);
      }

      grid.appendChild(dayCell);
    }
  }

  function shiftMonth(date, delta) {
    const absoluteMonth = date.year * 12 + date.month + delta;
    return {
      year: Math.floor(absoluteMonth / 12),
      month: ((absoluteMonth % 12) + 12) % 12,
    };
  }

  function isSameMonth(date, monthDate) {
    return date.year === monthDate.year && date.month === monthDate.month;
  }

  function isSameMonthOrBefore(left, right) {
    return left.year < right.year
      || (left.year === right.year && left.month <= right.month);
  }

  function isSameMonthOrAfter(left, right) {
    return left.year > right.year
      || (left.year === right.year && left.month >= right.month);
  }

  function getBusinessDateParts(date) {
    const values = BUSINESS_PARTS_FORMATTER.formatToParts(date).reduce((accumulator, part) => {
      if (part.type !== "literal") {
        accumulator[part.type] = part.value;
      }
      return accumulator;
    }, {});

    return {
      year: Number.parseInt(values.year, 10),
      month: Number.parseInt(values.month, 10) - 1,
      day: Number.parseInt(values.day, 10),
    };
  }

  function getDaysInMonth(year, month) {
    return new Date(Date.UTC(year, month + 1, 0, 12)).getUTCDate();
  }

  function getFirstDayOffset(year, month) {
    return (new Date(Date.UTC(year, month, 1, 12)).getUTCDay() + 6) % 7;
  }

  function formatMonthLabel(year, month) {
    return MONTH_LABEL_FORMATTER.format(new Date(Date.UTC(year, month, 1, 12)));
  }

  function formatTime(date) {
    return TIME_FORMATTER.format(date);
  }

  window.ClubsHubEventBrowser = { init };
  document.addEventListener("DOMContentLoaded", () => init(document));
})();
