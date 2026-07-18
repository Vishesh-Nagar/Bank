import React, { useState, useEffect } from "react";
import api from "../services/api";
import { AppLayout } from "../components/layout/AppLayout";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";

const AdminDashboard: React.FC = () => {
	const [tab, setTab] = useState(0);

	return (
		<AppLayout>
			<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full animate-modal-slide">
				<div className="flex items-center justify-between mb-8">
					<h1 className="text-3xl font-bold text-text-main">
						Admin Dashboard
					</h1>
				</div>

				<Card className="min-h-[500px]">
					<div className="flex border-b border-white/10 mb-6">
						<button
							className={`px-6 py-3 text-sm font-medium transition-colors border-b-2 ${
								tab === 0
									? "border-primary text-primary"
									: "border-transparent text-text-muted hover:text-white hover:border-white/20"
							}`}
							onClick={() => setTab(0)}
						>
							Users
						</button>
						<button
							className={`px-6 py-3 text-sm font-medium transition-colors border-b-2 ${
								tab === 1
									? "border-primary text-primary"
									: "border-transparent text-text-muted hover:text-white hover:border-white/20"
							}`}
							onClick={() => setTab(1)}
						>
							System Logs
						</button>
					</div>

					<div className="mt-4">
						{tab === 0 && <UsersTab />}
						{tab === 1 && <LogsTab />}
					</div>
				</Card>
			</div>
		</AppLayout>
	);
};

const UsersTab: React.FC = () => {
	const [users, setUsers] = useState<any[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	const fetchUsers = async () => {
		try {
			setLoading(true);
			const res = await api.get("/api/v1/users");
			setUsers(res.data.data.content || []);
		} catch (err: any) {
			setError("Failed to load users");
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchUsers();
	}, []);

	const toggleLock = async (id: number, currentStatus: boolean) => {
		try {
			await api.put(`/api/v1/users/${id}/lock`, null, {
				params: { locked: !currentStatus },
			});
			fetchUsers();
		} catch (err) {
			alert("Failed to update user lock status");
		}
	};

	if (loading) {
		return (
			<div className="flex justify-center p-8">
				<svg className="animate-spin h-8 w-8 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
					<circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
					<path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
				</svg>
			</div>
		);
	}
	if (error) {
		return (
			<div className="p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400">
				{error}
			</div>
		);
	}

	return (
		<div className="overflow-x-auto rounded-lg border border-white/10 bg-surface">
			<table className="w-full text-left text-sm whitespace-nowrap">
				<thead className="bg-white/5 uppercase text-text-muted border-b border-white/10">
					<tr>
						<th className="px-6 py-4 font-semibold">ID</th>
						<th className="px-6 py-4 font-semibold">Username</th>
						<th className="px-6 py-4 font-semibold">Email</th>
						<th className="px-6 py-4 font-semibold">Role</th>
						<th className="px-6 py-4 font-semibold">Locked?</th>
						<th className="px-6 py-4 font-semibold">Action</th>
					</tr>
				</thead>
				<tbody className="divide-y divide-white/10">
					{users.map((u) => (
						<tr key={u.id} className="hover:bg-white/5 transition-colors">
							<td className="px-6 py-4 text-white">{u.id}</td>
							<td className="px-6 py-4 text-white">{u.username}</td>
							<td className="px-6 py-4 text-white">{u.email}</td>
							<td className="px-6 py-4">
								<span className="px-2 py-1 text-xs font-medium bg-primary/20 text-primary-hover rounded-full border border-primary/30">
									{u.role}
								</span>
							</td>
							<td className="px-6 py-4">
								{u.accountLocked ? (
									<span className="text-red-400 font-medium">Yes</span>
								) : (
									<span className="text-emerald-400 font-medium">No</span>
								)}
							</td>
							<td className="px-6 py-4">
								<Button
									size="sm"
									variant={u.accountLocked ? "primary" : "danger"}
									onClick={() => toggleLock(u.id, u.accountLocked)}
								>
									{u.accountLocked ? "Unlock" : "Lock"}
								</Button>
							</td>
						</tr>
					))}
					{users.length === 0 && (
						<tr>
							<td colSpan={6} className="px-6 py-8 text-center text-text-muted">
								No users found.
							</td>
						</tr>
					)}
				</tbody>
			</table>
		</div>
	);
};

const LogsTab: React.FC = () => {
	const [serviceName, setServiceName] = useState("user-service");
	const [logs, setLogs] = useState<string[]>([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const fetchLogs = async () => {
		try {
			setLoading(true);
			setError(null);
			const res = await api.get("/api/v1/admin/logs", {
				params: { serviceName, lines: 200 },
			});
			setLogs(res.data.data || []);
		} catch (err: any) {
			setError(err.response?.data?.message || "Failed to load logs");
			setLogs([]);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchLogs();
	}, [serviceName]);

	return (
		<div>
			<div className="flex flex-wrap items-center gap-2 mb-6">
				{[
					"user-service",
					"account-service",
					"payment-service",
					"notification-service",
				].map((s) => (
					<Button
						key={s}
						variant={serviceName === s ? "primary" : "secondary"}
						size="sm"
						onClick={() => setServiceName(s)}
					>
						{s}
					</Button>
				))}
				<button 
					onClick={fetchLogs} 
					className="ml-auto text-sm font-medium text-text-muted hover:text-white flex items-center gap-2 transition-colors"
				>
					<svg className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
					</svg>
					Refresh
				</button>
			</div>

			{error && (
				<div className="mb-4 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400">
					{error}
				</div>
			)}

			<div className="bg-black border border-white/10 rounded-lg p-4 max-h-[60vh] overflow-auto font-mono text-xs text-emerald-400 relative">
				{loading && logs.length === 0 && (
					<div className="text-emerald-400/50 flex items-center gap-2">
						<span className="animate-pulse">Loading logs...</span>
					</div>
				)}
				{logs.length === 0 && !loading && (
					<div className="text-text-muted">No logs available for this service.</div>
				)}
				{logs.map((l, i) => (
					<div
						key={i}
						className="whitespace-pre-wrap break-all hover:bg-white/5 px-2 py-0.5 rounded transition-colors"
					>
						{l}
					</div>
				))}
			</div>
		</div>
	);
};

export default AdminDashboard;

