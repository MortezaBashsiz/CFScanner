import ast
import pathlib
import warnings

MAIN_PY = pathlib.Path(__file__).parent.parent / "src" / "cfscanner" / "main.py"


def test_main_module_compiles_without_syntax_warnings():
    """``main.py`` must not contain invalid escape sequences in string literals.

    Regression test for the ASCII-art logo being defined as a plain (non-raw)
    triple-quoted string containing backslashes, which raises a
    ``SyntaxWarning`` today and will become a ``SyntaxError`` in a future
    CPython version.
    """
    source = MAIN_PY.read_text()

    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        compile(source, str(MAIN_PY), "exec", ast.PyCF_ONLY_AST)

    syntax_warnings = [w for w in caught if issubclass(w.category, SyntaxWarning)]
    assert not syntax_warnings, [str(w.message) for w in syntax_warnings]
