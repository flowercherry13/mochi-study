// Mochi Study 🍡 — встроенный ИИ-помощник
// Этот файл обращается к уже настроенному Cloudflare Worker.
// Секретный DeepSeek API key хранится только в Cloudflare и сюда не добавляется.
(() => {
  const API = "https://mochi-study-api.agnia3333333.workers.dev";
  const box = document.getElementById("mochiAiHistory");
  const input = document.getElementById("mochiAiQuestion");
  const send = document.getElementById("mochiAiSend");
  const status = document.getElementById("mochiAiStatus");
  if (!box || !input || !send || !status) return;

  let messages = [];
  const render = () => {
    box.replaceChildren();
    if (!messages.length) {
      box.textContent = "Привет! Я помогу разобраться с китайским 🌸";
      return;
    }
    for (const m of messages) {
      const p = document.createElement("p");
      p.className = m.role === "user" ? "mochi-ai-user" : "mochi-ai-assistant";
      p.textContent = (m.role === "user" ? "Ты: " : "Помощник: ") + m.content;
      box.appendChild(p);
    }
    box.scrollTop = box.scrollHeight;
  };

  async function ask() {
    const question = input.value.trim();
    if (!question || send.disabled) return;
    messages.push({ role: "user", content: question });
    input.value = "";
    render();
    send.disabled = true;
    status.textContent = "Думаю…";
    try {
      const response = await fetch(API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ messages })
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.error || `Ошибка сервера: ${response.status}`);
      const answer = data.choices?.[0]?.message?.content || data.answer || data.response;
      if (!answer) throw new Error("Сервис вернул пустой ответ");
      messages.push({ role: "assistant", content: answer });
      render();
      status.textContent = "Готово ✨";
    } catch (error) {
      messages.pop();
      render();
      status.textContent = "Не получилось отправить запрос: " + error.message;
    } finally {
      send.disabled = false;
      input.focus();
    }
  }
  send.addEventListener("click", ask);
  input.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      ask();
    }
  });
  render();
})();
