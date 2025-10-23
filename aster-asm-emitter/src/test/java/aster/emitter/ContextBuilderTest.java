package aster.emitter;

import aster.core.ir.CoreModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 ContextBuilder 对枚举与数据类型索引的构建与查询能力。
 */
class ContextBuilderTest {

  private ContextBuilder builder;

  @BeforeEach
  void setUp() {
    var module = new CoreModel.Module();
    module.name = "std.collections";
    module.decls = new ArrayList<>();

    var statusEnum = new CoreModel.Enum();
    statusEnum.name = "ResultState";
    statusEnum.variants = List.of("Ok", "Err");
    module.decls.add(statusEnum);

    var listData = new CoreModel.Data();
    listData.name = "LinkedList";
    listData.fields = List.of();
    module.decls.add(listData);

    builder = new ContextBuilder(module);
  }

  @Test
  void testLookupDataType() {
    var dataByPkg = builder.lookupData("std.collections", "LinkedList");
    var dataBySimple = builder.lookupData("LinkedList");
    assertNotNull(dataByPkg);
    assertNotNull(dataBySimple);
    assertSame(dataByPkg, dataBySimple);
  }

  @Test
  void testLookupEnumType() {
    var enByPkg = builder.lookupEnum("std.collections", "ResultState");
    var enByFull = builder.lookupEnum("std.collections.ResultState");
    assertNotNull(enByPkg);
    assertNotNull(enByFull);
    assertSame(enByPkg, enByFull);
  }

  @Test
  void testGetEnumVariants() {
    var variants = builder.getEnumVariants("std.collections.ResultState");
    assertNotNull(variants);
    assertEquals(List.of("Ok", "Err"), variants);
    assertThrows(UnsupportedOperationException.class, () -> variants.add("New"));
    assertEquals("std.collections.ResultState", builder.findEnumOwner("Ok"));
  }

  @Test
  void testLookupNonExistent() {
    assertNull(builder.lookupData("std.collections", "MissingData"));
    assertNull(builder.lookupEnum("std.collections", "MissingEnum"));
    assertNull(builder.getEnumVariants("std.collections.MissingEnum"));
    assertNull(builder.findEnumOwner("MissingVariant"));
  }
}

