(function () {
  async function loadCaptcha() {
    const question = document.getElementById("captcha-question");
    const id = document.getElementById("captcha-id");
    try {
      const response = await fetch("/api/auth/captcha");
      if (!response.ok) throw new Error("Не удалось загрузить проверочный вопрос");
      const challenge = await response.json();
      question.textContent = challenge.question;
      id.value = challenge.id;
      document.getElementById("captcha-answer").value = "";
    } catch (error) { question.textContent = error.message; }
  }
  document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("feedback-form");
    document.getElementById("captcha-refresh").addEventListener("click", loadCaptcha);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      const button = form.querySelector('button[type="submit"]');
      button.disabled = true;
      try {
        const payload = {
          message: document.getElementById("feedback-message").value.trim(),
          rating: document.getElementById("feedback-rating").value,
          email: document.getElementById("feedback-email").value.trim(),
          website: document.getElementById("feedback-website").value,
          captchaId: document.getElementById("captcha-id").value,
          captchaAnswer: document.getElementById("captcha-answer").value.trim()
        };
        const response = await fetch("/api/feedback", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
        const result = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(result.error || "Не удалось отправить сообщение");
        showToast(result.message || "Спасибо за обратную связь!");
        form.reset();
        await loadCaptcha();
      } catch (error) { showToast(error.message, "error"); await loadCaptcha(); }
      finally { button.disabled = false; }
    });
    loadCaptcha();
  });
})();
