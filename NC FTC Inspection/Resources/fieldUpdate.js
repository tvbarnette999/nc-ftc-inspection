function update(){
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "field?team="+event.target.name+"&value="+event.target.value, true);
	xhttp.send();
}