import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLInputElement;

void main() {
    var document = HTMLDocument.current();

    var inputs = document.createElement("div");
    document.getBody().appendChild(inputs);
    inputs.appendChild(document.createTextNode("Your name: "));

    var input = (HTMLInputElement) document.createElement("input");
    input.setType("text");
    inputs.appendChild(input);

    var button = (HTMLButtonElement) document.createElement("button");
    button.appendChild(document.createTextNode("Greet"));
    inputs.appendChild(button);

    button.onClick(_ -> {
        var output = document.createElement("div");
        output.appendChild(document.createTextNode("Hello, " + input.getValue() + "!"));
        document.getBody().appendChild(output);
    });
}

