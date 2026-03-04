package com.example.dacs3.connectDB

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(
    supabaseUrl = "https://lhtytvfpuwbcghesagcm.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxodHl0dmZwdXdiY2doZXNhZ2NtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI0MzI5NTIsImV4cCI6MjA4ODAwODk1Mn0.jxOTqxZhshHfY_2ldBWVoC6sJB1T1-lmVeMbJoxCuMk"
) {
    install(Postgrest)
    install(Storage)
}