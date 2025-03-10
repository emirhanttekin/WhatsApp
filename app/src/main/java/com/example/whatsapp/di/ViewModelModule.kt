package com.example.whatsapp.di

import androidx.lifecycle.ViewModel
import com.example.whatsapp.ui.company.viewmodel.CreateGroupViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(CreateGroupViewModel::class)
    abstract fun bindCreateGroupViewModel(viewModel: CreateGroupViewModel): ViewModel

}
