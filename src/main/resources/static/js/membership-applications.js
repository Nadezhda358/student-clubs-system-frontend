document.addEventListener("DOMContentLoaded", function () {
  const page = document.getElementById("membershipApplicationsPage");
  if (!page) {
    return;
  }

  const table = document.getElementById("membershipAppsTable");
  const modal = document.getElementById("membershipActionModal");
  const modalMessage = document.getElementById("membershipModalMessage");
  const modalStudent = document.getElementById("membershipModalStudent");
  const modalClub = document.getElementById("membershipModalClub");
  const modalCancel = document.getElementById("membershipModalCancel");
  const modalConfirm = document.getElementById("membershipModalConfirm");
  const toastContainer = document.getElementById("toastContainer");
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
  const logoUrl = toastContainer?.dataset.logoUrl || "/assets/logo.png";
  const actionBasePath = normalizeBasePath(page.dataset.actionBasePath || "");
  const successMessage = page.dataset.successMessage || "Membership application updated successfully.";
  const hasDataRows = document.querySelectorAll("#membershipAppsTable tbody tr[data-app-row='true']").length > 0;

  let pendingAction = null;
  let lastFocusedElement = null;

  if (table && window.jQuery && window.jQuery.fn && window.jQuery.fn.DataTable) {
    window.jQuery("#membershipAppsTable").DataTable({
      paging: hasDataRows,
      searching: hasDataRows,
      ordering: hasDataRows,
      pageLength: 10,
      lengthMenu: [10, 25, 50, 100],
      order: [[0, "desc"]],
      columnDefs: [
        { targets: 5, orderable: false },
        { targets: 6, orderable: false }
      ]
    });
  }

  document.querySelectorAll(".js-membership-action").forEach((button) => {
    button.addEventListener("click", function (event) {
      event.preventDefault();

      const actionLabel = button.dataset.actionLabel || "update";
      pendingAction = {
        id: button.dataset.id || "",
        action: button.dataset.action || "",
        actionLabel: actionLabel,
        student: button.dataset.student || "-",
        club: button.dataset.club || "-"
      };

      if (modalMessage) {
        modalMessage.textContent = "You are about to " + actionLabel + " this membership application.";
      }
      if (modalStudent) {
        modalStudent.textContent = pendingAction.student;
      }
      if (modalClub) {
        modalClub.textContent = pendingAction.club;
      }

      openModal();
    });
  });

  modalCancel?.addEventListener("click", function (event) {
    event.preventDefault();
    closeModal();
  });

  modal?.addEventListener("click", function (event) {
    if (event.target === modal) {
      closeModal();
    }
  });

  modalConfirm?.addEventListener("click", async function (event) {
    event.preventDefault();
    if (!pendingAction || !pendingAction.id || !pendingAction.action || !actionBasePath) {
      closeModal();
      return;
    }

    try {
      const headers = { Accept: "application/json" };
      if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
      }

      const endpoint =
        actionBasePath +
        "/" +
        encodeURIComponent(pendingAction.id) +
        "/" +
        encodeURIComponent(pendingAction.action);

      const response = await fetch(endpoint, {
        method: "POST",
        credentials: "same-origin",
        headers: headers
      });

      if (!response.ok) {
        throw new Error(await readErrorMessage(response));
      }

      closeModal();
      showToast("success", successMessage);
      window.setTimeout(function () {
        window.location.reload();
      }, 300);
    } catch (error) {
      closeModal();
      showToast("error", error?.message || "Unable to update membership application.");
    }
  });

  function openModal() {
    if (!modal) {
      return;
    }

    lastFocusedElement = document.activeElement;
    modal.hidden = false;
    modal.classList.add("is-open");
    modal.setAttribute("aria-hidden", "false");
    modalConfirm?.classList.remove("is-approve", "is-reject");

    if (pendingAction?.action === "approve") {
      modalConfirm?.classList.add("is-approve");
      if (modalConfirm) {
        modalConfirm.textContent = "Confirm Approval";
      }
    } else if (pendingAction?.action === "reject") {
      modalConfirm?.classList.add("is-reject");
      if (modalConfirm) {
        modalConfirm.textContent = "Confirm Rejection";
      }
    } else if (modalConfirm) {
      modalConfirm.textContent = "Confirm";
    }

    document.addEventListener("keydown", handleModalKeydown);
    window.setTimeout(function () {
      modalConfirm?.focus();
    }, 0);
  }

  function closeModal() {
    if (!modal) {
      return;
    }

    modal.classList.remove("is-open");
    modal.hidden = true;
    modal.setAttribute("aria-hidden", "true");
    pendingAction = null;
    modalConfirm?.classList.remove("is-approve", "is-reject");
    if (modalConfirm) {
      modalConfirm.textContent = "Confirm";
    }
    document.removeEventListener("keydown", handleModalKeydown);

    if (lastFocusedElement && typeof lastFocusedElement.focus === "function") {
      lastFocusedElement.focus();
    }
  }

  function handleModalKeydown(event) {
    if (!modal || modal.hidden) {
      return;
    }

    if (event.key === "Escape") {
      event.preventDefault();
      closeModal();
      return;
    }

    if (event.key !== "Tab") {
      return;
    }

    const focusable = [modalCancel, modalConfirm].filter((node) => node && !node.disabled);
    if (!focusable.length) {
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  async function readErrorMessage(response) {
    const fallback = "Unable to update membership application.";
    const text = await response.text();
    if (!text || !text.trim()) {
      return fallback;
    }

    try {
      const parsed = JSON.parse(text);
      if (parsed && typeof parsed === "object") {
        if (typeof parsed.message === "string" && parsed.message.trim()) {
          return parsed.message.trim();
        }
        if (typeof parsed.error === "string" && parsed.error.trim()) {
          return parsed.error.trim();
        }
        if (typeof parsed.detail === "string" && parsed.detail.trim()) {
          return parsed.detail.trim();
        }
      }
    } catch (_error) {
      return text.trim();
    }

    return text.trim();
  }

  function showToast(type, message) {
    if (!toastContainer) {
      return;
    }

    const tone = ["success", "error", "warning", "info"].includes(type) ? type : "info";
    const title = tone === "success"
      ? "Success"
      : tone === "error"
        ? "Error"
        : tone === "warning"
          ? "Warning"
          : "Info";

    const toast = document.createElement("div");
    toast.className = "toast-item toast-item--" + tone;
    toast.innerHTML =
      '<img class="toast-item__logo" src="' + escapeHtml(logoUrl) + '" alt="Logo" />' +
      '<div class="toast-item__body">' +
      '<p class="toast-item__title">' + escapeHtml(title) + "</p>" +
      '<p class="toast-item__text">' + escapeHtml(message) + "</p>" +
      "</div>";
    toastContainer.appendChild(toast);

    window.setTimeout(function () {
      toast.classList.add("is-hiding");
      window.setTimeout(function () {
        toast.remove();
      }, 230);
    }, 3000);
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function normalizeBasePath(value) {
    return value.replace(/\/+$/, "");
  }
});
