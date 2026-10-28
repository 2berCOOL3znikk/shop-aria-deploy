(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  let verificationStatus = "NOT_SUBMITTED";

  async function request(url, options = {}) {
    const response = await ariaAuth.authFetch(url, options);
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.error || body.message || "Запрос не выполнен");
    return body;
  }

  function ensureVerificationPanel() {
    if (document.getElementById("org-verification-panel")) return;
    const panel = document.createElement("section");
    panel.id = "org-verification-panel";
    panel.className = "glass-card org-verification-panel";
    panel.innerHTML = '<h2>Проверка организации</h2><p id="org-verification-status" class="admin-note"></p><form id="org-verification-form"><label for="org-verification-name">Название организации</label><input id="org-verification-name" name="organizationName" required maxlength="160"><label for="org-registration-number">Регистрационный номер</label><input id="org-registration-number" name="registrationNumber" required maxlength="100"><button class="btn-glow" type="submit">Отправить на проверку</button></form>';
    document.querySelector(".social-hero")?.after(panel);
    panel.querySelector("form").addEventListener("submit", async event => {
      event.preventDefault();
      const form = event.currentTarget;
      try {
        await request("/api/organizations/verification", {
          method: "POST", headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ organizationName: form.organizationName.value.trim(), registrationNumber: form.registrationNumber.value.trim() })
        });
        showToast("Заявка отправлена администратору");
        await loadVerification();
        await loadOffers();
      } catch (error) { showToast(error.message, "error"); }
    });
  }

  async function loadVerification() {
    const role = ariaAuth.getRole();
    const message = document.getElementById("partner-message");
    if (role !== "ORG") {
      if (message) message.textContent = ariaAuth.isLoggedIn() ? "Доступ по льготе — войдите как партнёр." : "Доступ по льготе — войдите как партнёр.";
      return;
    }
    ensureVerificationPanel();
    const form = document.getElementById("org-verification-form");
    const status = document.getElementById("org-verification-status");
    try {
      const result = await request("/api/organizations/verification");
      verificationStatus = result.status || "NOT_SUBMITTED";
      if (result.organizationName) document.getElementById("org-verification-name").value = result.organizationName;
      const messages = {
        NOT_SUBMITTED: "Подтвердите организацию, чтобы бронировать лоты.",
        PENDING_VERIFICATION: "Заявка на проверке. Бронирование станет доступно после решения администратора.",
        VERIFIED: "Организация проверена. Теперь можно бронировать доступные лоты.",
        REJECTED: "Заявка отклонена. Исправьте данные и отправьте её повторно."
      };
      status.textContent = messages[verificationStatus] || "Статус проверки: " + verificationStatus;
      form.hidden = verificationStatus === "PENDING_VERIFICATION" || verificationStatus === "VERIFIED";
      if (message) message.textContent = verificationStatus === "VERIFIED" ? "Организация проверена: бронирование доступно." : "Сначала отправьте заявку на проверку организации.";
      const proof=document.querySelector('.social-proof');
      if(proof) proof.textContent=verificationStatus==='VERIFIED'?'После бронирования скачайте акт и согласуйте выдачу. Передачу подтверждает сотрудник магазина.':'Отправьте данные организации на проверку. После подтверждения станет доступно бронирование.';
    } catch (error) { status.textContent = error.message; }
  }

  function offerCard(offer) {
    const urgent = offer.expiryDate && Math.ceil((new Date(offer.expiryDate + "T23:59:59") - new Date()) / 86400000) <= 3;
    let action = '<p class="partner-access-note">Доступ по льготе — войдите как партнёр.</p>';
    if (ariaAuth.getRole() === "ORG") {
      action = verificationStatus === "VERIFIED"
        ? '<button class="btn-glow" onclick="claimOffer(' + Number(offer.id) + ')">Забронировать · ' + Number(offer.quantityAvailable) + ' шт.</button>'
        : '<p class="partner-access-note">Бронирование доступно проверенным организациям.</p>';
    }
    return '<article class="offer-card">' + CatalogImages.imageTag({ productName: offer.productName, productSku: offer.productSku, category: offer.category, imageUrl: offer.imageUrl }, { alt: offer.productName }) + '<div class="offer-body"><div class="product-card-category">' + (urgent ? "Приоритетный лот" : "Соц. полка") + '</div><h3 class="offer-title">' + escape(offer.productName) + '</h3><div class="offer-meta">Количество: <strong>' + Number(offer.quantityAvailable) + ' шт.</strong><br>Предложение до: ' + (offer.validUntil ? new Date(offer.validUntil).toLocaleDateString("ru-KZ") : "не указано") + '<br>Годен до: ' + (offer.expiryDate ? new Date(offer.expiryDate).toLocaleDateString("ru-KZ") : "не указан") + '</div><div class="offer-reason">' + escape(offer.ariaReason || "Позиция отобрана для предотвращения списания") + '</div><div class="offer-price">Передача без оплаты</div>' + action + '</div></article>';
  }

  async function loadOffers() {
    const grid = document.getElementById("grid");
    try {
      const response = await fetch("/api/social/offers");
      if (!response.ok) throw new Error("Не удалось загрузить предложения");
      const offers = await response.json();
      offers.sort((a, b) => (a.expiryDate || "9999-12-31").localeCompare(b.expiryDate || "9999-12-31"));
      grid.innerHTML = offers.length ? offers.map(offerCard).join("") : '<div class="empty-state glass-card" style="grid-column:1/-1"><span aria-hidden="true">♧</span><h3>Пока нет опубликованных лотов</h3><p>Администратор подтвердит публикацию после проверки товара и срока годности.</p></div>';
    } catch (error) { grid.innerHTML = '<div class="empty-state glass-card" style="grid-column:1/-1">' + escape(error.message) + '</div>'; }
  }

  async function loadClaims() {
    if (ariaAuth.getRole() !== "ORG" || verificationStatus !== "VERIFIED") return;
    const section = document.getElementById("claims-section");
    section.style.display = "block";
    try {
      const claims = await request("/api/social/my-claims");
      document.getElementById("claims-list").innerHTML = claims.length ? claims.map(c => '<div class="claim-row"><span><strong>' + escape(c.productName) + '</strong> · ' + (c.status==='TRANSFERRED'?'передано':'забронировано') + ' ' + Number(c.claimedQuantity || 0) + ' шт.</span><button class="btn-outline" onclick="downloadClaimAct(' + Number(c.id) + ')">Акт передачи</button></div>').join("") : '<p class="admin-note">Забронированных лотов пока нет.</p>';
    } catch (_) { document.getElementById("claims-list").textContent = "Не удалось загрузить бронирования."; }
  }

  window.claimOffer = async id => {
    if (ariaAuth.getRole() !== "ORG") return;
    if (verificationStatus !== "VERIFIED") { showToast("Сначала дождитесь проверки организации", "error"); return; }
    try {
      await request("/api/organizations/offers/" + encodeURIComponent(id) + "/claim", { method: "POST" });
      showToast("Лот забронирован");
      await loadOffers();
      await loadClaims();
    } catch (error) { showToast(error.message, "error"); }
  };
  window.loadSocial = loadOffers;
  window.renderOffer = offerCard;
  window.loadClaims = loadClaims;
  window.downloadClaimAct = async id => {
    try {
      const response = await ariaAuth.authFetch("/api/social/my-claims/" + encodeURIComponent(id) + "/act");
      if (!response.ok) throw new Error("Акт не найден");
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement("a"); link.href = url; link.download = "aria-social-act-" + id + ".html";
      document.body.appendChild(link); link.click(); link.remove(); setTimeout(() => URL.revokeObjectURL(url), 30000);
    } catch (error) { showToast(error.message, "error"); }
  };
  document.addEventListener("DOMContentLoaded", async () => {
    await loadVerification();
    await loadOffers();
    await loadClaims();
  });
})();
