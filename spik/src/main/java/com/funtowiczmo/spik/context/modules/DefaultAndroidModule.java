package com.funtowiczmo.spik.context.modules;

import com.funtowiczmo.spik.repositories.ContactRepository;
import com.funtowiczmo.spik.repositories.MessageRepository;
import com.funtowiczmo.spik.repositories.impl.DefaultContactRepository;
import com.funtowiczmo.spik.repositories.impl.SmartMessageRepository;
import com.google.inject.AbstractModule;

/**
 * Created by momo- on 28/10/2015.
 */
public class DefaultAndroidModule extends AbstractModule {


    @Override
    protected void configure() {
        bind(ContactRepository.class).to(DefaultContactRepository.class);
        bind(MessageRepository.class).to(SmartMessageRepository.class);
    }
}
