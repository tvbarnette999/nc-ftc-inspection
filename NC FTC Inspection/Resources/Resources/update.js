function update(){
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "update?team="+event.target.name+"&value="+event.target.value, true);
	xhttp.send();
	var num = event.target.name;
	var r = document.getElementById("R"+num.substring(0, num.indexOf("_")))
	var c = "#FFFFFF";
	switch(parseInt(event.target.value)){
	case 1: 
		c = "#FF0000";
		break;
	case 2:
		c = "#00FFFF";
		break;
	case 3:
		c = "#00FF00";
	default:
		break;
	}
	r.bgColor = c;
}
