<!DOCTYPE php>
<html>
	<body >
	<? echo $_SERVER["REMOTE_ADDR"]; ?>
		<h2>This Page is Secured: </h2>
		
		<form method="post">
			<input type="hidden" name="data" value="data"></input>
			Password: <input type="password" name="password">
			<input type="hidden" name="secret" value="secret">
			<input type="submit" value="submit"/>
		</form>	
		
	</body>
</html>