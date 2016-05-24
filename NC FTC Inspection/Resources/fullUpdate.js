function update(){
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../fullupdate?team="+event.target.name+"&value="+event.target.checked, true);
	xhttp.send();
}

function fullpass(){
	//check everything is passed first.
	//get signitures
	//TODO send note
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../update?team="+event.target.name+"&value=3", true);
	xhttp.send();
}

function fullfail(){
	//TODO send note
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../update?team="+event.target.name+"&value=1", true);
	xhttp.send();
	sendNote();
}

function sendNote(){
	var n=document.getElementById("note");
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../note?team="+n.name, true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("XX".concat(n.value.concat("&&&")));
}