package com.alibaba.oneagent.service;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author hengyunabc 2020-07-30
 *
 */
public class OneAgentClassFileTransformer implements ClassFileTransformer {
    private static final Logger logger = LoggerFactory.getLogger(OneAgentClassFileTransformer.class);
    private TransformerManager transformerManager;
    private boolean canRetransform;

    public OneAgentClassFileTransformer(TransformerManager transformerManager, boolean canRetransform) {
        this.transformerManager = transformerManager;
        this.canRetransform = canRetransform;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        List<ClassFileTransformer> transformers = transformerManager.classFileTransformer(canRetransform);

        for (ClassFileTransformer transformer : transformers) {
            byte[] transformResult = null;
            try {
                transformResult = transformer.transform(loader, className, classBeingRedefined, protectionDomain,
                        classfileBuffer);
                // TODO 增加 dump 结果到本地磁盘，方便调试

            } catch (Throwable e) {
                logger.error("transform error, loader: {}, className: {}, transformer: {}", loader, className,
                        transformer);
            }
            if (transformResult != null) {
                classfileBuffer = transformResult;
            }
        }

        return classfileBuffer;
    }
}
