function submitForm() {
    fetch("/", {
            method: "POST",
            body: JSON.stringify({username: nameInput.value, message: msgInput.value, replyTo: replyInput.value})
        }
    ).then(response => response.json())
        .then(json => {
            if (json["success"]) msgInput.value = ""
            errorDiv.innerText = json["err"]
        })
    return false
}

let socket = new WebSocket("ws://" + location.host + "/subscribe");
socket.onmessage = function (ev) {
    messageList.innerHTML = ev.data
}

function submitFilter() {
    socket.send(filterInput.value)
    return false
}