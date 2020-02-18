<html>
<head>
  <title>Welcome!</title>
</head>
<body>
  <h1>Welcome ${user}!</h1>
  <p/>
  <#list eventList as eventItem>
  <div>
    <span>${eventItem.id}</span>
    <span>${eventItem.eventId}</span>
  </div>
  </#list>

</body>
</html>
