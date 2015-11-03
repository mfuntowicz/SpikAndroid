package com.funtowiczmo.spik.modules;

import com.funtowiczmo.spik.repositories.ContactRepository;
import com.funtowiczmo.spik.repositories.MessageRepository;
import com.funtowiczmo.spik.repositories.impl.DefaultContactRepository;
import com.funtowiczmo.spik.repositories.impl.DefaultMessageRepository;
import com.google.inject.AbstractModule;

/**
 * Created by momo- on 28/10/2015.
 */
public class DefaultAndroidModule extends AbstractModule {


    @Override
    protected void configure() {
        bind(ContactRepository.class).to(DefaultContactRepository.class);
        bind(MessageRepository.class).to(DefaultMessageRepository.class);
    }
}
