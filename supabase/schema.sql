-- Run in the Supabase SQL Editor for NearShare Phase 3.
create extension if not exists pgcrypto;
create table if not exists public.listings (
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null references auth.users(id) on delete cascade,
 title text not null check (char_length(title) between 1 and 80),
 description text not null check (char_length(description) between 1 and 1000),
 category text not null,
 listing_type text not null check (listing_type in ('Borrow','Give away')),
 item_condition text not null check (item_condition in ('New','Like new','Good','Fair','For parts')),
 location_text text not null check (char_length(location_text) between 1 and 120),
 image_url text,
 status text not null default 'active' check (status in ('active','paused','completed')),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
create index if not exists listings_active_created_at_idx on public.listings (created_at desc) where status='active';
create index if not exists listings_owner_id_idx on public.listings (owner_id);
alter table public.listings enable row level security;
drop policy if exists "Anyone can read active listings" on public.listings;
create policy "Anyone can read active listings" on public.listings for select using (status='active' or auth.uid()=owner_id);
drop policy if exists "Members can create their own listings" on public.listings;
create policy "Members can create their own listings" on public.listings for insert to authenticated with check (auth.uid()=owner_id);
drop policy if exists "Members can update their own listings" on public.listings;
create policy "Members can update their own listings" on public.listings for update to authenticated using (auth.uid()=owner_id) with check (auth.uid()=owner_id);
drop policy if exists "Members can delete their own listings" on public.listings;
create policy "Members can delete their own listings" on public.listings for delete to authenticated using (auth.uid()=owner_id);
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('nearshare-listing-images','nearshare-listing-images',true,8388608,array['image/jpeg','image/png','image/webp'])
on conflict(id) do update set public=true,file_size_limit=8388608,allowed_mime_types=array['image/jpeg','image/png','image/webp'];
drop policy if exists "Public can view listing images" on storage.objects;
create policy "Public can view listing images" on storage.objects for select using(bucket_id='nearshare-listing-images');
drop policy if exists "Members upload images to their own folder" on storage.objects;
create policy "Members upload images to their own folder" on storage.objects for insert to authenticated with check(bucket_id='nearshare-listing-images' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Members update images in their own folder" on storage.objects;
create policy "Members update images in their own folder" on storage.objects for update to authenticated using(bucket_id='nearshare-listing-images' and (storage.foldername(name))[1]=auth.uid()::text) with check(bucket_id='nearshare-listing-images' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Members delete images in their own folder" on storage.objects;
create policy "Members delete images in their own folder" on storage.objects for delete to authenticated using(bucket_id='nearshare-listing-images' and (storage.foldername(name))[1]=auth.uid()::text);
