package io.bit3.jsass;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import io.bit3.jsass.context.Context;
import io.bit3.jsass.importer.Import;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ImporterTest {

  private Compiler compiler;
  private Options  options;

  /**
   * Set up the compiler and the compiler options for each run.
   *
   * @throws URISyntaxException Throws if the resource URI is invalid.
   */
  @Before
  public void setUp() throws IOException, URISyntaxException {
    compiler = new Compiler();
    options = new Options();
  }

  @Test
  public void testCall() throws Exception {
    Functions functions = new Functions();
    Importer  importer  = new Importer();

    options.getFunctionProviders().add(functions);
    options.getImporters().add(importer);

    Output output = compiler.compileString(
        "foo { bar: func(); } @import 'import.scss';",
        new URI("input.scss"),
        new URI("output.css"),
        options
    );

    assertFalse(functions.importPaths.isEmpty());
    assertFalse(importer.importPaths.isEmpty());

    assertArrayEquals(
        new String[]{
            "input.scss",
            "import.scss",
            "imported.scss",
            "import.scss"
        },
        functions.importPaths.toArray()
    );

    assertArrayEquals(
        new String[]{
            "input.scss",
            "import.scss"
        },
        importer.importPaths.toArray()
    );
  }

  public static class Functions {

    protected List<String> importPaths = new LinkedList<>();

    public String func(Import lastImport) {
      this.importPaths.add(lastImport.getUri().toString());
      return "World";
    }
  }

  public static class Importer implements io.bit3.jsass.importer.Importer {

    protected List<String> importPaths = new LinkedList<>();

    @Override
    public Collection<Import> apply(
        String url, Import previous, Context originalContext
    ) {
      this.importPaths.add(previous.getUri().toString());

      List<Import> imports = new LinkedList<>();

      if ("import.scss".equals(url)) {
        try {
          imports.add(
              new Import(
                  new URI("import.scss"),
                  new URI(""),
                  "foo { bar: func(); } @import 'imported.scss'; bar { foo: func(); }"
              )
          );
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
      }

      if ("imported.scss".equals(url)) {
        try {
          imports.add(
              new Import(
                  new URI("imported.scss"),
                  new URI(""),
                  "foo { bar: func(); }"
              )
          );
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
      }

      return imports;
    }
  }
}
