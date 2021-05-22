function submitForm() {
    let req = parentIdInput.value === "" ? JSON.stringify({
        name: nameInput.value,
        msg: msgInput.value
    }) : JSON.stringify({name: nameInput.value, msg: msgInput.value, replyTo: parseInt(parentIdInput.value)})
    fetch("/", {
            method: "POST",
            body: req
        }
    ).then(response => response.json())
        .then(json => {
            if (json["success"]) msgInput.value = ""
            errorDiv.innerText = json["err"]
        })
    return false;
}

function filter() {
    socket.send(filterInput.value)
    return false;
}

var socket = new WebSocket("ws://" + location.host + "/subscribe");
socket.onmessage = function (ev) {
    console.log("HI!")
    messageList.innerHTML = ev.data
}