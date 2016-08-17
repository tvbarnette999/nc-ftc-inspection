function update(){
	var xhttp = new XMLHttpRequest();
	document.getElementById("BG"+event.target.name).bgColor = "#000000";
	//response text should be BG+id=v
	xhttp.onreadystatechange = function() {
		 if (xhttp.readyState == 4 && xhttp.status == 200){
			  var resp = xhttp.responseText;
			  console.log(resp)
			  var ind =  resp.indexOf("=");
			  var id = resp.substring(0,ind);
			  var v = resp.substring(ind + 1);
			  console.log(id);
			  console.log(id.substring(2));
			  console.log(v);
			  console.log(document.getElementsByName(id.substring(2))[0].checked)
			  if(eval(v) == document.getElementsByName(id.substring(2))[0].checked){
				  document.getElementById(id).bgColor = "#00FF00";
			  
			  }
		}
	};
	xhttp.open("POST", "../fullupdate?team="+event.target.name+"&value="+event.target.checked, true);
	xhttp.send();
	
}

function fullpass(){
	
	
	sendNote();
	//check everything is passed first. If not, popup and do not post.
	var inputs=document.getElementsByTagName("input");
	var length = inputs.length;
	var allPass=true;
	
	//if HW,ask for cube index. We will skip checking that index's checkbox as it will be handled by the cube page
	//if cube inspection not separate from hw, server will return -1.
	var name = event.target.name;
	var ind = name.indexOf("_");
	var isHW = name.substring(ind+1, ind+3) == "HW";
	var cubeIndex=-1;
	
	if(isHW){
		var xhttp = new XMLHttpRequest();
		xhttp.open("POST", "../cubeindex?",false);
		xhttp.send();
		var cubeIndex=parseInt(xhttp.responseText);
	}
	for (var i = 0; i < length; i++) {
		if(i==cubeIndex)continue; //skip the sizing cube
	    if(inputs[i].type=="checkbox" && !inputs[i].checked){
	    	allPass=false;
	    	break;
	    }
	}
	if(allPass){
	
		//TODO get signatures.
		var teamsig = prompt("I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of the FIRST Tech Challenge have been abided by.\nTeam Student Representative:");
		if(teamsig == null || teamsig==""){
			return;
		}
		
		var inspectorsig = prompt("I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of the FIRST Tech Challenge have been abided by.\nInspector:");
		if(inspectorsig == null || inspectorsig==""){
			return;
		}
		var xhttp = new XMLHttpRequest();
		xhttp.open("POST", "../sig?team="+event.target.name, true);
		xhttp.send("XXteam=".concat(teamsig.concat("&inspector=".concat(inspectorsig.concat("&&&")))));
		
		xhttp = new XMLHttpRequest();
		xhttp.open("POST", "../update?team="+event.target.name+"&value=3", true);
		xhttp.send();
	}else{
		window.alert("Not all items have passed!");
	}
}

function fullfail(){
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