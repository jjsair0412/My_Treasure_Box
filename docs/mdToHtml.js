
  // Markdown 파일을 읽어와 HTML로 변환하는 함수
  function convertMarkdownToHTML(markdownFile, callback) {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if (xhr.readyState === 4 && xhr.status === 200) {
        var markdownText = xhr.responseText;
        var htmlText = marked(markdownText);
        callback(htmlText);
      }
    };
    xhr.open("GET", markdownFile, true);
    xhr.send();
  }

  // Markdown 파일의 경로
  var markdownFile = "/AWS/README.md";

  // 변환된 HTML을 적용하는 함수
  function displayMarkdownAsHTML(htmlText) {
    var container = document.getElementById("markdown-container");
    container.innerHTML = htmlText;
  }

  // Markdown 파일을 읽어와 HTML로 변환하고 표시하는 코드 실행
  convertMarkdownToHTML(markdownFile, displayMarkdownAsHTML);

