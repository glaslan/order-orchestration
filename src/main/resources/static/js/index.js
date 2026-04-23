/**
 * PRODUCT LIST PAGE LOGIC
 */

function getCsrfToken() {
  return document.cookie.split('; ')
      .find(c => c.startsWith('XSRF-TOKEN='))
      ?.split('=')[1];
}

document.addEventListener("DOMContentLoaded", () => {
  // Intercept add-to-cart forms
  document.querySelectorAll("form").forEach((form) => {
    const action = form.getAttribute("action");
    if (!action || !action.includes("/addToCart")) return;

    form.addEventListener("submit", function (e) {
      e.preventDefault();

      const qtyInput = this.querySelector('input[name="quantity"]');
      const val = parseInt(qtyInput.value);
      const max = parseInt(qtyInput.getAttribute("max"));

      if (val > max) {
        showToast(`Only ${max} in stock`, "danger");
        qtyInput.value = 1;
        return;
      }

      const fd = new URLSearchParams(new FormData(this));
      fetch("/addToCart", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "X-XSRF-TOKEN": getCsrfToken()
        },
        body: fd,
      })
        .then(async (r) => {
          const txt = await r.text();
          if (r.ok) {
            showToast("Added to cart", "success");
            qtyInput.value = 1;
            const btnMinus = this.querySelector('input[value="-"]');
            if (btnMinus) btnMinus.disabled = true;
          } else {
            showToast(txt, "danger");
          }
        })
        .catch(() => showToast("Connection Error", "danger"));
    });
  });

  // Quantity +/- button behavior
  document
    .querySelectorAll('.input-group input[type="button"]')
    .forEach((btn) => {
      btn.addEventListener("click", function () {
        const group = this.closest(".input-group");
        const input = group.querySelector('input[name="quantity"]');
        const btnMinus = group.querySelector('input[value="-"]');
        const btnPlus = group.querySelector('input[value="+"]');

        let current = parseInt(input.value);
        const max = parseInt(input.getAttribute("max") || "9999");
        const min = parseInt(input.getAttribute("min") || "1");

        if (this.value === "+") {
          if (current < max) input.value = ++current;
        } else {
          if (current > min) input.value = --current;
        }

        if (btnMinus) btnMinus.disabled = current <= min;
        if (btnPlus) btnPlus.disabled = current >= max;
      });
    });

  // Manual input validation
  document.querySelectorAll('input[name="quantity"]').forEach((input) => {
    input.addEventListener("input", function () {
      const val = parseInt(this.value);
      const max = parseInt(this.getAttribute("max") || "9999");
      const min = parseInt(this.getAttribute("min") || "1");

      const group = this.closest(".input-group");
      if (group) {
        const btnMinus = group.querySelector('input[value="-"]');
        const btnPlus = group.querySelector('input[value="+"]');
        if (btnMinus) btnMinus.disabled = val <= min;
        if (btnPlus) btnPlus.disabled = val >= max;
      }
    });
  });
});

function showToast(message, type = "success") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast align-items-center text-white bg-${type} border-0 mb-2`;
  toast.setAttribute("role", "alert");
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body">${message}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
    </div>
  `;
  container.prepend(toast);

  if (window.bootstrap && bootstrap.Toast) {
    new bootstrap.Toast(toast, { delay: 2400 }).show();
    toast.addEventListener("hidden.bs.toast", () => toast.remove());
  } else {
    setTimeout(() => toast.remove(), 2500);
  }
}
