package club.sk1er.commandpatcher.tweaker;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class ClassTransformer implements IClassTransformer {

    private final Logger LOGGER = LogManager.getLogger("Command Patcher");

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;

        if (transformedName.equals("net.minecraftforge.client.ClientCommandHandler")) {
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.EXPAND_FRAMES);

            for (MethodNode methodNode : node.methods) {
                String methodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, methodNode.name, methodNode.desc);

                if (methodName.equals("executeCommand") || methodName.equals("func_71556_a")) {
                    methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), checkCommandString());
                    break;
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

            try {
                node.accept(writer);
            } catch (Throwable t) {
                LOGGER.error("Exception when transforming {} : {}", transformedName, t.getClass().getSimpleName());
                t.printStackTrace();
            }

            return writer.toByteArray();
        }

        return bytes;
    }

    private InsnList checkCommandString() {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 2));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim", "()Ljava/lang/String;", false));
        list.add(new LdcInsnNode("/"));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false));
        LabelNode ifne = new LabelNode();
        list.add(new JumpInsnNode(Opcodes.IFNE, ifne));
        list.add(new InsnNode(Opcodes.ICONST_0));
        list.add(new InsnNode(Opcodes.IRETURN));
        list.add(ifne);
        return list;
    }
}
