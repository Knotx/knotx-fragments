function filterNone() {
  return NodeFilter.FILTER_ACCEPT;
}

function getKnotXNodes(rootElem) {
  const COMMENTS = [];
  const COMMENT_NODE_CODE = 8;
  const HTML_NODE_CODE = 1;
  // Fourth argument, which is actually obsolete according to the DOM4 standard, is required in IE 11
  var iterator = document.createNodeIterator(
      rootElem,
      NodeFilter.SHOW_ALL,
      filterNone,
      false
  );

  var curNode;
  var isBetweenComments = false;
  var curComment = "";
  while ((curNode = iterator.nextNode())) {
    if (curNode.nodeType === COMMENT_NODE_CODE) {
      isBetweenComments = !isBetweenComments;
      curComment = isBetweenComments ? curNode.data.trim() : "";
    }

    if (
        isBetweenComments &&
        curNode.nodeType !== COMMENT_NODE_CODE &&
        curNode.nodeType === HTML_NODE_CODE
    ) {
      if (!COMMENTS.length) {
        curNode.dataset.knotxId = curComment.substr(15, 36);
        COMMENTS.push(curNode);
      } else if (!COMMENTS[COMMENTS.length - 1].contains(curNode)) {
        curNode.dataset.knotxId = curComment.substr(15, 36);
        COMMENTS.push(curNode);
      }
    }
  }
  return COMMENTS;
}

function bindEvents(knotXNodes) {
  knotXNodes.map(item => {
    item.addEventListener(
        "click",
        function(ev) {
          document.querySelectorAll(`[data-knotx-id]`).forEach(function(el) {
            el.style.outline = "";
          });
          document.querySelectorAll(`[data-knotx-id='${ev.currentTarget.dataset.knotxId}']`)
          .forEach(function(el) {
            if (el.style.outline.trim() === "") {
              el.style.outline = "1px solid orange";
            } else {
              el.style.outline = "";
            }
          });

          document.getElementById("knotx-fragment-body").innerHTML = "<h5>Original body</h5><xmp>" +
              debugData[ev.currentTarget.dataset.knotxId].body + "</xmp>";
          document.getElementById("knotx-fragment-payload").innerHTML = "<h5>Payload</h5><xmp>" +
              JSON.stringify(debugData[ev.currentTarget.dataset.knotxId].payload, null, 2) +
              "</xmp>";
          document.getElementById("knotx-fragment-logs").innerHTML = "<h5>Logs</h5><xmp>" +
              JSON.stringify(debugData[ev.currentTarget.dataset.knotxId].logs, null, 2) +
              "</xmp>";
        },
        true
    );
  });
}

function createTemplate() {
  var newDiv = document.createElement("div");

  newDiv.setAttribute("id", "knotx-debug-template");
  newDiv.innerHTML =
      "<h3>Knot.x Debug Console</h3><div id='knotx-fragment-body'></div><div id='knotx-fragment-payload'></div><div id='knotx-fragment-logs'></div>";

  document.body.appendChild(newDiv);
}

window.addEventListener("load", function() {
  const knotXNodes = getKnotXNodes(document.body);

  NodeList.prototype.forEach = Array.prototype.forEach;

  bindEvents(knotXNodes);
  createTemplate();
});