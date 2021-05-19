function submitForm() {
    fetch("/", {
            method: "POST",
            body: JSON.stringify({to: toInput.value, name: nameInput.value, msg: msgInput.value})
        }
    ).then(response => response.json())
        .then(json => {
            if (json["success"]) msgInput.value = ""
            errorDiv.innerText = json["err"]
        })
    return false;
}

function changeDisplayForm() {
   console.log("Change display form handler")
   fetch("/cascade", {
    method: "POST",
     body: JSON.stringify({ })
   }).then(response => response.json())
             .then(json => {
                 if (json["success"]) msgInput.value = ""
                 errorDiv.innerText = json["err"]
             })
    return false
}

function filterForm() {
    console.log("Filter form handler")
    fetch("/user-filter", {
            method: "POST",
            body: JSON.stringify({filterStr: filterInput.value})
        }
    ).then(response => response.json())
        .then(json => {
            if (json["success"]) msgInput.value = ""
            errorDiv.innerText = json["err"]
        })
    return false;
}

var socket = new WebSocket("ws://" + location.host + "/subscribe");
socket.onmessage = function (ev) {
    let data = ev.data
    console.log(data)
    if (data.split("#")[0] == "display") {
        displayMode.innerHTML = data.split("#")[1] == "true" ? "<h2>Display: Cascade</h2>" : "<h2>Display: Column</h2>"
    } else {
        messageList.innerHTML = ev.data
    }
}