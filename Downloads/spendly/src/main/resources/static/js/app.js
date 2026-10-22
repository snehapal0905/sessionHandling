// Spendly - small progressive enhancements
(function () {
  // Mobile sidebar
  document.addEventListener('click', function (e) {
    var toggle = e.target.closest('[data-menu-toggle]');
    if (toggle) {
      document.querySelector('.sidebar').classList.toggle('open');
    }
  });

  // Confirm before destructive actions: <form data-confirm="Are you sure?">
  document.addEventListener('submit', function (e) {
    var msg = e.target.getAttribute('data-confirm');
    if (msg && !window.confirm(msg)) {
      e.preventDefault();
    }
  });

  // Show / hide password
  document.querySelectorAll('[data-password-toggle]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var input = document.getElementById(btn.getAttribute('aria-controls'));
      var show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.setAttribute('aria-pressed', show);
      btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    });
  });

  // Income / expense / transfer switch: blocks marked data-show-for="INCOME EXPENSE" follow the checked type.
  // Hidden blocks have their fields disabled so only the visible category list is submitted.
  var switcher = document.querySelector('[data-type-switch]');
  if (switcher) {
    var sync = function () {
      var checked = switcher.querySelector('input:checked');
      var type = checked ? checked.value : '';
      document.querySelectorAll('[data-show-for]').forEach(function (block) {
        var on = block.getAttribute('data-show-for').split(' ').indexOf(type) >= 0;
        block.hidden = !on;
        block.querySelectorAll('input, select, textarea').forEach(function (f) { f.disabled = !on; });
      });
    };
    switcher.addEventListener('change', sync);
    sync();
  }

  // Colour pickers mirror their value into a text label: <input type=color data-color-label="#id">
  document.querySelectorAll('[data-color-label]').forEach(function (input) {
    var label = document.querySelector(input.getAttribute('data-color-label'));
    var update = function () { if (label) label.textContent = input.value; };
    input.addEventListener('input', update);
    update();
  });

  // Auto-submit filter forms when a select changes: <form data-autosubmit>
  document.querySelectorAll('form[data-autosubmit] select, form[data-autosubmit] input[type=month]').forEach(function (el) {
    el.addEventListener('change', function () { el.form.submit(); });
  });

  // Live total on the budget planner
  var planner = document.querySelector('[data-budget-planner]');
  if (planner) {
    var symbol = planner.getAttribute('data-currency') || '';
    var total = planner.querySelector('[data-budget-total]');
    var recalc = function () {
      var sum = 0;
      planner.querySelectorAll('input[data-budget]').forEach(function (i) { sum += parseFloat(i.value) || 0; });
      if (total) total.textContent = symbol + Math.round(sum).toLocaleString(symbol === '₹' ? 'en-IN' : 'en-US');
    };
    planner.addEventListener('input', recalc);
    recalc();
  }
})();
