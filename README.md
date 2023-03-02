# MicroJava-Compiler

Dieser Compiler wurde begleitend zur vorwissenschaftlichen Arbeit "Aufbau, Entwicklung und Funktionsweise eines Compilers für elementare Anwendungen in MicroJava" entwickelt. Es sind dabei Codestücke enthalten, die als Angabe zur Übung "Übersetzerbau" an der Johannes Kepler Univesität zur Verfügung gestellt wurden.

Der Compiler wurde mit dem JDK 1.8. entwickelt. Die zu übersetzende Datei ist in der Klasse Compiler.java zu spezivizieren. Die erzeugte Datei besitzt die Dateiendung .cmj ("compiled MicroJava". Die Output-Datei enthält eine Klartextrepräsentation der generierten Bytecodebefehle, die für Menschen verständlich ist und ist daher nicht ausführbar. 

Die Datei Test.mj beinhaltet ein Beispielprogramm in MicroJava, das korrekt übersetzt werden kann.
