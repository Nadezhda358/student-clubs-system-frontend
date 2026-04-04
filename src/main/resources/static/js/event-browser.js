(function () {
  function init(root) {
    const scope = root instanceof Element ? root : document;
    const browsers = [];

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
      .map((seed) => ({
        title: seed.dataset.title || "Event",
        club: seed.dataset.club || "",
        location: seed.dataset.location || "",
        href: seed.dataset.href || "#",
        start: seed.dataset.start ? new Date(seed.dataset.start) : null,
      }))
      .filter((event) => event.start instanceof Date && !Number.isNaN(event.start.getTime()))
      .sort((a, b) => a.start - b.start);

    const today = new Date();
    const baseDate = events.length > 0 ? events[0].start : today;
    const state = {
      currentMonth: new Date(baseDate.getFullYear(), baseDate.getMonth(), 1),
      minMonth:
        events.length > 0 ? new Date(events[0].start.getFullYear(), events[0].start.getMonth(), 1) : null,
      maxMonth:
        events.length > 0
          ? new Date(events[events.length - 1].start.getFullYear(), events[events.length - 1].start.getMonth(), 1)
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
    prev.textContent = "Previous";

    const label = document.createElement("p");
    label.className = "event-calendar__label";

    const next = document.createElement("button");
    next.type = "button";
    next.className = "event-calendar__nav";
    next.textContent = "Next";

    header.appendChild(prev);
    header.appendChild(label);
    header.appendChild(next);

    const weekdays = document.createElement("div");
    weekdays.className = "event-calendar__weekdays";
    ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].forEach((day) => {
      const cell = document.createElement("span");
      cell.textContent = day;
      weekdays.appendChild(cell);
    });

    const grid = document.createElement("div");
    grid.className = "event-calendar__grid";

    const empty = document.createElement("div");
    empty.className = "event-calendar__empty";
    empty.textContent = "No events scheduled for this month.";

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

    const monthStart = new Date(state.currentMonth.getFullYear(), state.currentMonth.getMonth(), 1);
    const monthEnd = new Date(state.currentMonth.getFullYear(), state.currentMonth.getMonth() + 1, 0);
    label.textContent = monthStart.toLocaleDateString(undefined, { month: "long", year: "numeric" });

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

    const monthEvents = state.events.filter((event) => isSameMonth(event.start, monthStart));
    empty.hidden = monthEvents.length > 0;

    const firstDayOffset = (monthStart.getDay() + 6) % 7;
    const totalDays = monthEnd.getDate();

    for (let i = 0; i < firstDayOffset; i += 1) {
      const filler = document.createElement("div");
      filler.className = "event-calendar__day event-calendar__day--filler";
      grid.appendChild(filler);
    }

    for (let day = 1; day <= totalDays; day += 1) {
      const current = new Date(monthStart.getFullYear(), monthStart.getMonth(), day);
      const dayCell = document.createElement("div");
      dayCell.className = "event-calendar__day";

      const number = document.createElement("span");
      number.className = "event-calendar__date";
      number.textContent = String(day);
      dayCell.appendChild(number);

      monthEvents
        .filter((event) => event.start.getDate() === day)
        .slice(0, 3)
        .forEach((event) => {
          const link = document.createElement("a");
          link.className = "event-calendar__chip";
          link.href = event.href;
          link.textContent = `${formatTime(event.start)} ${event.title}`;
          link.title = `${event.title}${event.club ? " • " + event.club : ""}`;
          dayCell.appendChild(link);
        });

      const hiddenCount = monthEvents.filter((event) => event.start.getDate() === day).length - 3;
      if (hiddenCount > 0) {
        const more = document.createElement("span");
        more.className = "event-calendar__more";
        more.textContent = `+${hiddenCount} more`;
        dayCell.appendChild(more);
      }

      grid.appendChild(dayCell);
    }
  }

  function shiftMonth(date, delta) {
    return new Date(date.getFullYear(), date.getMonth() + delta, 1);
  }

  function isSameMonth(date, monthDate) {
    return date.getFullYear() === monthDate.getFullYear() && date.getMonth() === monthDate.getMonth();
  }

  function isSameMonthOrBefore(left, right) {
    return left.getFullYear() < right.getFullYear()
      || (left.getFullYear() === right.getFullYear() && left.getMonth() <= right.getMonth());
  }

  function isSameMonthOrAfter(left, right) {
    return left.getFullYear() > right.getFullYear()
      || (left.getFullYear() === right.getFullYear() && left.getMonth() >= right.getMonth());
  }

  function formatTime(date) {
    return date.toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  window.ClubsHubEventBrowser = { init };
  document.addEventListener("DOMContentLoaded", () => init(document));
})();
