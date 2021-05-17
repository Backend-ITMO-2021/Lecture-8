var socket = new WebSocket("ws://" + location.host + "/subscribe");
socket.onmessage = function (ev) {
  messageList.innerHTML = ev.data;
};

function submitForm() {
  fetch("/", {
    method: "POST",
    body: JSON.stringify({
      username: nameInput.value,
      msg: msgInput.value,
      parentId: parentInput.value,
    }),
  })
    .then((response) => response.json())
    .then((json) => {
      if (json["success"]) msgInput.value = "";
      errorDiv.innerText = json["err"];
    });
  return false;
}

function submitFilter() {
  const value = filterInput.value.trim();
  socket.send(value ? `filter?=${value}` : "");
  return false;
}
