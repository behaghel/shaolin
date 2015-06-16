var button = document.getElementById("submit");
var code = document.getElementById("code");
var feedback = document.getElementById("feedback");
var checkAnswer = function() {
    var xhr = new XMLHttpRequest();
    xhr.onload = function() { feedback.textContent = this.responseText; };
    xhr.open("post", "/submission/Palindrome.java", true);
    xhr.send(code.value);
};
button.onclick = checkAnswer;