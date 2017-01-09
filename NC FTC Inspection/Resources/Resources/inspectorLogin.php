<!DOCTYPE php>
<html>
<script>
function test() {
	if (window.event.keyCode == 13) {
		sendPW();
	}
}
function sendPW() {
	var s = document.getElementById('pw').value;
	console.log(s);
	if (s.charAt(s.length - 1) == '\n') {
		s = s.substring(s, s.length - 1);
	}
	var count = 0;
	console.log('creating xhttp');
	var xhttp = new XMLHttpRequest();
	console.log('created xhttp');
	xhttp.onreadystatechange = function() {
		console.log('got response: (' + xhttp.responseText.length + ') ' + xhttp.responseText);
		eval(xhttp.responseText);
		//location.reload();
		window.location.assign(window.location.href);
//		window.location.reload()
// 		if (typeof xhttp.responseText != 'undefined' || xhttp.resposeText.length == 0) return;
// 		if (count++ == 0) {
// 			alert(xhttp.responseText);
// 			document.write(xhttp.responseText);
// 		}
	};
	var loc = window.location.href;
	loc = loc.substring(loc.lastIndexOf('/') + 1, loc.length);
	console.log("../" + loc + "?data=data&password="+s+"&secret=secret");
	xhttp.open("POST", "../" + loc + "?data=data&password="+s+"&secret=secret", true);
	xhttp.send();
}
</script>
	<body >
	<? echo $_SERVER["REMOTE_ADDR"]; ?>
		<h2>This Page is Secured: </h2>
		
<!-- 		<form method="post"> -->
<!-- 			<input type="hidden" name="data" value="data"></input> -->
<!-- 			Password: <input type="password" name="password"> -->
<!-- 			<input type="hidden" name="secret" value="secret"> -->
<!-- 			<input type="submit" value="submit"/> -->
			
<!-- 		</form>	 -->
		<!-- We are shifting to this new system to avoid the problem with going back pages -->
		<input onkeypress="test()" cols="20" rows="1" id="pw" type="password" autofocus></input>
		<button onclick="sendPW()">Submit</button>
		
	</body>
</html>