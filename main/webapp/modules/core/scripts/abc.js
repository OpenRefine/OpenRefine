function fetchRedirectedUrl(url) {
    var xhr = new XMLHttpRequest();
    xhr.open("HEAD", url, false);
    xhr.send();
    if (xhr.status == 200) {
      return xhr.responseURL;
    }
  }
  
  function addColumnByFetchingRedirectedUrl(column, newColumnName) {
    var newColumn = [];
    for (var i = 0; i < column.length; i++) {
      var url = column[i];
      var redirectedUrl = fetchRedirectedUrl(url);
      newColumn.push(redirectedUrl);
    }
    var newColumnHeader = {
      name: newColumnName,
      type: "string",
    };
    return {
      rows: [newColumn],
      columnHeaders: [newColumnHeader],
    };
  }