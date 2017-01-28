function update(){
	var xhttp = new XMLHttpRequest();
	event.target.bgColor = "#FF0000"; 
	checked = !eval(event.target.getAttribute("checked"));
	event.target.setAttribute("checked", checked);
	console.log(event.target.id +", " + checked)
	if(checked){
		event.target.innerHTML = " \u2713 ";
	} else{
		event.target.innerHTML = "   ";
	}
	//response text should be id=v
	xhttp.onreadystatechange = function() {
		 if (xhttp.readyState == 4 && xhttp.status == 200){
			  var resp = xhttp.responseText;
			  var ind =  resp.indexOf("=");
			  var id = resp.substring(0,ind);
			  var v = resp.substring(ind + 1);
			  if(eval(v) == eval(document.getElementById(id).getAttribute("checked"))){// document.getElementsByName(id.substring(2))[0].checked){
				  document.getElementById(id).bgColor = "#FFFFFF";
			  }
		}
	};
	xhttp.open("POST", "../fullupdate?team="+event.target.id+"&value="+event.target.getAttribute("checked"), true);
	xhttp.send();
	
}

function fullpass(){
	
	//TODO check only the given team's in multi
	sendNote();
	//check everything is passed first. If not, popup and do not post. (Checks only required ones)
	var inputs = document.getElementsByClassName("REQ");
	var length = inputs.length;
	var allPass = true;
	console.log(length);
	//if HW,ask for cube index. We will skip checking that index's checkbox as it will be handled by the cube page
	//if cube inspection not separate from hw, server will return -1.
	var name = event.target.name;
	var ind = name.indexOf("_");
	var isHW = name.substring(ind + 1, ind + 3) == "HW";
	var number = eval(name.substring(0, ind));
	console.log("STUFF: "+ number+", "+name);
	var cubeIndex=-1;
	
	if(isHW){
		var xhttp = new XMLHttpRequest();
		xhttp.open("POST", "../cubeindex?",false);
		xhttp.send();
		var cubeIndex=parseInt(xhttp.responseText);
	}
	for (var i = 0; i < length; i++) {
		if(i == cubeIndex)continue; //skip the sizing cube
		//check value again 0 (required)
		console.log(inputs[i].id + ", " + inputs[i].getAttribute("checked") + ", "+ number);
	    if( number == eval(inputs[i].id.substring(0, inputs[i].id.indexOf("_"))) && !eval(inputs[i].getAttribute("checked"))){ 
	    	allPass = false;
	    	break;
	    }
	}
	if(allPass){
	
		xhttp = new XMLHttpRequest();
		xhttp.open("POST", "../update?team="+event.target.name+"&value=3", true);
		xhttp.send();
		//TODO get signatures.
		var teamsig = document.getElementById("sig_0").value;//prompt("I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of the FIRST Tech Challenge have been abided by.\nTeam Student Representative:");
		if(teamsig == null || teamsig==""){
			return;
		}
		
		var inspectorsig = document.getElementById("sig_1").value;// prompt("I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of the FIRST Tech Challenge have been abided by.\nInspector:");
		if(inspectorsig == null || inspectorsig==""){
			return;
		}
		var xhttp = new XMLHttpRequest();
		xhttp.open("POST", "../sig?team="+event.target.name, true);
		xhttp.send("XXteam=".concat(teamsig.concat("&inspector=".concat(inspectorsig.concat("&&&")))));
		
		
		
		window.location.reload(true);
	}else{
		window.alert("Not all items have passed!");
	}
}

function fullfail(){
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../update?team="+event.target.name+"&value=1", true);
	xhttp.send();
	sendNote();
	window.location.reload(true);
}

function sendNote(){
	var n=document.getElementById("note");
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "../note?team="+n.name, true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("XX".concat(n.value.concat("&&&")));
}