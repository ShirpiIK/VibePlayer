package com.vibeplayer.app.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import com.vibeplayer.app.lyrics.LyricsCacheDao
import com.vibeplayer.app.lyrics.VibeDatabase
import com.vibeplayer.app.player.PlayerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.vibeplayer.app.lyrics.LrcLibApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
        PlayerFactory.create(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideLrcLibApi(retrofit: Retrofit): LrcLibApi =
        retrofit.create(LrcLibApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VibeDatabase =
        Room.databaseBuilder(context, VibeDatabase::class.java, "vibe_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLyricsCacheDao(db: VibeDatabase): LyricsCacheDao = db.lyricsCacheDao()
}
