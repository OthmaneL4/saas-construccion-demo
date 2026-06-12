(() => {
    const formatAmount = (value) => {
        const amount = Number(value);
        return Number.isFinite(amount) ? amount.toFixed(2) : '';
    };

    const bindPaymentAmountHelper = () => {
        const invoiceSelect = document.getElementById('invoiceId');
        const amountInput = document.getElementById('amount');
        const amountHelp = document.getElementById('amountHelp');
        if (!invoiceSelect || !amountInput) {
            return;
        }

        const selectedOutstanding = () => {
            const selectedOption = invoiceSelect.options[invoiceSelect.selectedIndex];
            return selectedOption ? selectedOption.dataset.outstanding : '';
        };

        const updateHelp = (formattedAmount) => {
            if (!amountHelp || !formattedAmount) {
                return;
            }
            const prefix = amountHelp.textContent.includes('Disponible')
                    ? 'Disponible para esta factura'
                    : 'Pendiente de esta factura';
            amountHelp.textContent = `${prefix}: EUR ${formattedAmount}`;
        };

        const fillAmount = () => {
            const outstanding = selectedOutstanding();
            const formattedAmount = formatAmount(outstanding);
            if (!formattedAmount) {
                return;
            }
            amountInput.value = formattedAmount;
            updateHelp(formattedAmount);
        };

        invoiceSelect.addEventListener('change', fillAmount);
        updateHelp(formatAmount(selectedOutstanding()));
    };

    const bindArchiveConfirmation = () => {
        document.addEventListener('submit', (event) => {
            const form = event.target;
            if (!(form instanceof HTMLFormElement)) {
                return;
            }
            const actionPath = new URL(form.action, window.location.origin).pathname;
            if (!actionPath.endsWith('/archive')) {
                return;
            }
            const message = form.dataset.confirm
                    || 'Vas a archivar este registro. Dejara de mostrarse en los listados activos. Quieres continuar?';
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    };

    document.addEventListener('DOMContentLoaded', () => {
        bindPaymentAmountHelper();
        bindArchiveConfirmation();
    });
})();
