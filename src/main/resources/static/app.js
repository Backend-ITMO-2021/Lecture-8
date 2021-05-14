function submitForm() {
    fetch("/", {
            method: "POST",
            body: JSON.stringify({name: nameInput.value, msg: msgInput.value, reply: replyInput.value})
        }
    ).then(response => response.json())
        .then(json => {
            if (json["success"]) msgInput.value = ""
            errorDiv.innerText = json["err"]
        })
    return false;
}

function submitFilter() {
    fetch("/filter", {
            method: "POST",
            body: JSON.stringify({username: filterInput.value})
        }
    ).then(response => response.json())
        .then(json => {
            errorDiv.innerText = json["err"]
        })
    return false;
}

var socket = new WebSocket("ws://" + location.host + "/subscribe");
socket.onmessage = function (ev) {
    messageList.innerHTML = ev.data
}